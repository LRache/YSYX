#include <chrono>

#include "memory.h"
#include "debug.h"
#include "hdb.h"
#include "difftest.h"
#include "test_img.h"
#include "config.h"
#include "perf.h"
#include "trace.h"
#include "utils.hpp"

CPU cpu;
uint32_t lastPC;
uint32_t lastInst;

VTop top;
std::chrono::time_point<std::chrono::system_clock> timerStart;
static uint64_t timer = 0;

#define IMG_NAME test_img_no_hazard_addi
static uint32_t *img = IMG_NAME;
static size_t img_size = sizeof(IMG_NAME);

static void exec_once() {
    top.clock = 0; top.eval();
    top.clock = 1; top.eval();
    cpu.clockCount ++;
    nvboard::update();
}

void hdb::init() {
    difftest::init();
    nvboard::init();

    load_img_to_flash_from_mem(img, img_size);
    if (config::loadRom) load_img_to_rom_from_file(config::romImgFileName);
    if (config::loadFlash) load_img_to_flash_from_file(config::flashImgFileName);

    cpu.running = true;
    cpu.instCount = 0;
    timer = 0;
    cpu.clockCount = 0;

    top.reset = 1;
    for (int i = 0; i < 16; i++) exec_once();
    top.reset = 0;
    Log("Reset at clock=%" PRIu64, cpu.clockCount);
    
    perf::init();
    trace::open();

    cpu.mstatus = 0x1800;
    Log("Init finished.");
}

void hdb::step() {
    if (!cpu.running) {
        Log("Is not running!");
        return;
    }
    exec_once();
    while (!cpu.done && cpu.running) {
        exec_once();
    }
    if (cpu.running) difftest::step();
    cpu.instCount++;
    if (!(in_flash(cpu.pc) || in_sdram(cpu.pc))) {
        panic("Invalid PC = " FMT_WORD", lastPC = " FMT_WORD, cpu.pc, lastPC);
    }
    // Log("STEP " FMT_WORD, cpu.pc);
}

void hdb_statistic() {
    auto timerEnd = std::chrono::high_resolution_clock::now();
    timer += std::chrono::duration_cast<std::chrono::microseconds>(timerEnd - timerStart).count();
    uint64_t realTimer = cpu.clockCount * 1000000 / REAL_FREQ;

    Log("Total count of instructions = %" PRIu64 " with %" PRIu64 " clocks, IPC=%.6lf", cpu.instCount, cpu.clockCount, (double)cpu.instCount / cpu.clockCount);
    Log("Total time spent = %'" PRIu64 " us(%s), frequency=%.3lfkHz", timer, us_to_text(timer).c_str(), (double)cpu.clockCount * 1000 / timer);
    Log("Total time spent in reality = %'" PRIu64 " us(%s)", realTimer, us_to_text(realTimer).c_str());
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
    trace::close();
    return r;
}

void hdb::ebreak() {
    Log("ebreak at pc=" FMT_WORD, cpu.pc);
    cpu.running = false;
}

void hdb::invalid_inst() {
    cpu.running = false;
    panic("Invalid Inst at pc=" FMT_WORD " (inst=" FMT_WORD ") clock=%lu", cpu.pc, cpu.inst, cpu.clockCount);
}

void hdb::set_gpr(uint32_t addr, word_t data) {
    // Log("Set gpr x%d = " FMT_WORD "(%d) at pc=" FMT_WORD "(inst=" FMT_WORD ")", addr, data, data, cpu.pc, cpu.inst);
    if (addr != 0) cpu.gpr[addr] = data;
}

const char *name = "unknown";
void hdb::set_csr(uint32_t addr, word_t data) {
    if (addr == 0) return ;
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

void hdb::set_pc(word_t pc) {
    if (!cpu.running) return ;
    
    lastPC = cpu.pc;
    cpu.pc = pc;
    itrace::trace(pc);
    // Log("Exec to pc=" FMT_WORD " at clock=%lu", pc, cpu.clockCount);
}

void hdb::set_inst(word_t inst) {
    lastInst = cpu.inst;
    cpu.inst = inst;
    // Log(FMT_WORD, inst);
}

void hdb::set_done(bool done) {
    cpu.done = done;
}
