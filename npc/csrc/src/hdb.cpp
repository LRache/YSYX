#include <iostream>
#include <fstream>
#include <chrono>
#include <queue>

#include "memory.h"
#include "debug.h"
#include "hdb.h"
#include "difftest.h"
#include "test_img.h"
#include "config.h"
#include "nvboard.h"
#include "perf.h"
#include "itrace.h"

CPU cpu;
uint32_t lastPC;
uint32_t lastInst;

VTop top;
std::chrono::time_point<std::chrono::system_clock> timerStart;
static uint64_t timer = 0;
std::string hdb::outputDir = "./";

#define IMG_NAME test_img_ecall
static uint32_t *img = IMG_NAME;
static size_t img_size = sizeof(IMG_NAME);

static void exec_once() {
    top.clock = 0; top.eval();
    top.clock = 1; top.eval();
    cpu.clockCount ++;
    // Log("Exec once");
    nvboard::update();
}

void hdb::init(
    const std::string &memImgPath, 
    const std::string &romImgPath, 
    const std::string &flashImgPath, 
    const std::string &outputDir
) {
    difftest::init();
    nvboard::init();

    load_img_to_flash_from_mem(img, img_size);
    load_img_to_mem_from_file(memImgPath);
    load_img_to_rom_from_file(romImgPath);
    load_img_to_flash_from_file(flashImgPath);
    hdb::outputDir = outputDir;

    cpu.running = true;
    cpu.instCount = 0;
    timer = 0;
    cpu.clockCount = 0;

    top.reset = 1;
    for (int i = 0; i < 16; i++) exec_once();
    top.reset = 0;
    Log("Reset at clock=%lu", cpu.clockCount);
    
    perf::init();
    itrace::start(INST_START);

    cpu.mstatus = 0x1800;
    Log("Init finished.");
}

void hdb::step() {
    if (!cpu.running) {
        Log("Is not running!");
        return;
    }
    exec_once();
    while (!cpu.valid && cpu.running) {
        exec_once();
    }
    // Log("T");
    if (cpu.running) difftest::step();
    cpu.instCount++;
}

void hdb_statistic() {
    auto timerEnd = std::chrono::high_resolution_clock::now();
    timer += std::chrono::duration_cast<std::chrono::microseconds>(timerEnd - timerStart).count();

    Log("Total count of instructions = %" PRIu64 " with %" PRIu64 " clocks, IPC=%.6lf", cpu.instCount, cpu.clockCount, (double)cpu.instCount / cpu.clockCount);
    Log("Total time spent = %'" PRIu64 " us, frequency=%.3lfkHz", timer, (double)cpu.clockCount * 1000 / timer);
    if (timer > 0) Log("Simulation frequency = %'" PRIu64 " clocks/s", cpu.clockCount * 1000000 / timer);
}

void hdb::end() {
    difftest::end();
    perf::statistic();
    hdb_statistic();
}

int hdb::run(uint64_t n) {
    timerStart = std::chrono::high_resolution_clock::now();
    if (n == 0) {
        while (cpu.running) step();
    } else {
        while (cpu.running && n--) step();
    }
    
    int r = cpu.gpr[10];
    if (r == 0) {
        Log(ANSI_FG_GREEN "HIT GOOD TRAP" ANSI_FG_BLUE " at pc=" FMT_WORD, cpu.pc);
    } else {
        Log(ANSI_FG_RED "HIT BAD TRAP" ANSI_FG_BLUE " with code %d at pc=" FMT_WORD, r, cpu.pc);
    }
    itrace::end();
    if (!hdb::outputDir.empty()) {
        itrace::dump_to_file(hdb::outputDir + "//itrace.bin");
    }
    return r;
}

void hdb_set_csr(uint32_t addr, word_t data) {
    if (addr == 0) return ;
    const char *name = "unknown";
    switch (addr)
    {
        case 2: cpu.satp     = data; name = "satp"; break;
        case 3: cpu.mstatus  = data; name = "mstatus"; break;
        case 4: cpu.mtvec    = data; name = "mtvec"; break;
        case 5: cpu.mscratch = data; name = "mscratch"; break;
        case 6: cpu.mepc     = data; name = "mepc"; break;
        case 7: cpu.mcause   = data; name = "mcause"; break;
        default: panic("Invalid CSR: %d at pc=0x%08x(inst=0x%08x)", addr, cpu.pc, cpu.inst);
    }
    // Log("Set csr %d[%s]=" FMT_WORD " at pc=" FMT_WORD, addr, name, data, cpu.pc);
}

void hdb_set_gpr(uint32_t addr, word_t data) {
    // Log("Set gpr x%d = " FMT_WORD "(%d) at pc=" FMT_WORD "(inst=" FMT_WORD ")", addr, data, data, cpu.pc, cpu.inst);
    if (addr != 0) cpu.gpr[addr] = data;
}

void hdb_invalid_inst() {
    cpu.running = false;
    panic("Invalid Inst at pc=" FMT_WORD " (inst=" FMT_WORD ") clock=%lu", cpu.pc, cpu.inst, cpu.clockCount);
}

void hdb_update_pc(uint32_t pc) {
    if (!cpu.running) return ;
    
    if (!(top.reset || in_flash(pc) || in_sdram(pc))) {
        panic("Invalid PC = " FMT_WORD", lastPC = " FMT_WORD, pc, lastPC);
    }
    lastPC = cpu.pc;
    cpu.pc = pc;
    itrace::trace(pc);
    // Log("Exec to pc=" FMT_WORD " at clock=%lu", pc, cpu.clockCount);
}

void hdb_update_inst(uint32_t inst) {
    lastInst = cpu.inst;
    cpu.inst = inst;
    // Log(FMT_WORD, inst);
}

void hdb_update_valid(bool valid) {
    cpu.valid = valid;
}

extern "C" {
    void set_gpr(uint32_t addr, word_t data) {
        hdb_set_gpr(addr, data);
    }

    void set_csr(uint32_t addr, uint32_t data) {
        hdb_set_csr(addr, data);
    }

    void update_reset(uint8_t reset) {}

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