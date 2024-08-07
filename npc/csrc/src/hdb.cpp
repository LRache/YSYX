#include <iostream>
#include <fstream>
#include <chrono>

#include "memory.h"
#include "debug.h"
#include "hdb.h"
#include "difftest.h"
#include "test_img.h"
#include "config.h"

CPU cpu;
uint32_t lastPC;
uint32_t lastInst;
uint32_t regs[32];

VTop top;
static uint64_t instCount = 0;
static uint64_t timer = 0;
static uint64_t clockCount = 0;

#define IMG_NAME test_img_uart

static uint32_t *img = IMG_NAME;
static size_t img_size = sizeof(IMG_NAME);

void load_img_from_file(std::string path) {
    std::ifstream f;
    f.open(path, std::ios::binary);
    uint32_t addr = INST_START;
    while (!f.eof()) {
        uint32_t buf;
        f.read((char *)&buf, 4);
        mem_write(addr, buf, 4);
        set_rom(addr, buf);
        difftest_memcpy(addr, &buf, 4, DIFFTEST_TO_REF);
        addr += 4;
    }
    f.close();
}

void load_img_from_mem() {
    uint32_t addr = INST_START;
    for (int i = 0; i < img_size / 4; i++) {
        mem_write(addr, img[i], 4);
        set_rom(addr, img[i]);
        addr += 4;
    }
    difftest_memcpy(INST_START, img, img_size, DIFFTEST_TO_REF);
}

static void exec_once() {
    top.clock = 0; top.eval();
    top.clock = 1; top.eval();
    clockCount ++;
}

void hdb_init(std::string imgPath) {
    difftest_init();
    if (imgPath.empty()) {
        load_img_from_mem();
    } else {
        load_img_from_file(imgPath);
        std::cout << "Load IMG " << imgPath << std::endl;
    }
    char str[28] = "Hello World! from flash.\n";
    addr_t addr = FLASH_BASE;
    for (int i = 0; i < 7; i++) {
        set_flash(addr, *((word_t *)str + i));
        addr += 4;
    }

    cpu.running = true;
    instCount = 0;
    timer = 0;
    clockCount = 0;

    top.reset = 1;
    for (int i = 0; i < 16; i++) exec_once();
    top.reset = 0;
    Log("Reset.");
    
    cpu.mstatus = 0x1800;
    Log("Init finished.");
    // for (int i = 0; i < 20; i++) {
    //     exec_once();
    //     Log("Exec to pc=" FMT_WORD, cpu.pc);
    //     Log();
    // }
}

void hdb_step() {
    if (!cpu.running) {
        Log("Is not running!");
        return;
    }

    while (!cpu.valid && cpu.running)
    {
        exec_once();
    }
    exec_once(); // update PC for difftest.
    // Log("Exec to pc=" FMT_WORD, top.io_pc);
    difftest_step();

    instCount++;
}

void hdb_statistic() {
    Log("Total count of instructions = %'" PRIu64, instCount);
    Log("Total time spent = %'" PRIu64 " us with %" PRIu64 " clocks", timer, clockCount);
    if (timer > 0) Log("Simulation frequency = %'" PRIu64 " inst/s", instCount * 1000000 / timer);
}

int hdb_run(uint64_t n) {
    auto timerStart = std::chrono::high_resolution_clock::now();
    if (n == 0) {
        while (cpu.running)
        {
            hdb_step();
        }
    } else {
        while (cpu.running && n--)
        {
            hdb_step();
        }
    }
    auto timerEnd = std::chrono::high_resolution_clock::now();
    timer += std::chrono::duration_cast<std::chrono::microseconds>(timerEnd - timerStart).count();
    
    int r = cpu.gpr[10];
    if (r == 0) {
        Log(ANSI_FG_GREEN "HIT GOOD TRAP" ANSI_FG_BLUE " at pc=" FMT_WORD, cpu.pc);
    } else {
        Log(ANSI_FG_RED "HIT BAD TRAP" ANSI_FG_BLUE " with code %d at pc=" FMT_WORD, r, cpu.pc);
    }
    difftest_end();
    hdb_statistic();
    return r;
}

void hdb_set_csr(uint32_t addr, uint32_t data) {
    switch (addr)
    {
        case 0x180: cpu.satp    = data; break;
        case 0x300: cpu.mstatus = data; break;
        case 0x305: cpu.mtvec   = data; break;
        case 0x340: cpu.mscratch= data; break;
        case 0x341: cpu.mepc    = data; break;
        case 0x342: cpu.mcause  = data; break;
        default: panic("Invalid CSR: 0x%x(%d) at pc=0x%08x(inst=0x%08x)", addr, addr, cpu.pc, cpu.inst);
    }
    // Log("Set CSR addr=0x%x, data=0x%08x(%d)", addr, data, data);
}

void hdb_invalid_inst() {
    cpu.running = false;
    panic("Invalid Inst at pc=%x\ninst=0x%08x", cpu.pc, cpu.inst);
}

void hdb_update_pc(uint32_t pc) {
    lastPC = cpu.pc;
    cpu.pc = pc;
}

void hdb_update_inst(uint32_t inst) {
    lastInst = cpu.inst;
    cpu.inst = inst;
}

void hdb_update_valid(bool valid) {
    cpu.valid = valid;
}

extern "C" {
    void set_reg(uint32_t addr, uint32_t data) {
        // Log("Set register addr=%d, data=" FMT_WORD "(%d)\npc=" FMT_WORD "(inst=" FMT_WORD ")", addr, data, data, cpu.pc, cpu.inst);
        cpu.gpr[addr] = data;
    }

    void set_csr(uint32_t addr, uint32_t data) {
        hdb_set_csr(addr, data);
    }

    void update_reset(uint8_t reset) {
    }

    void update_pc(uint32_t pc) {
        hdb_update_pc(pc);
    }

    void update_inst(uint32_t inst) {
        hdb_update_inst(inst);
    }

    void update_valid(uint8_t valid) {
        hdb_update_valid(valid);
    }

    void env_break() {
        std::cout << "ebreak" << std::endl;
        cpu.running = false;
    }

    void invalid_inst() {
        hdb_invalid_inst();
    }
}