#include "config.h"

#include "difftest.h"
#include "debug.h"
#include "hdb.h"
#include "memory.h"
#include "macro.h"

#ifdef DIFFTEST

uint32_t refRegs[DIFFTEST_COMMON_REG_COUNT];
uint32_t refCSR[DIFFTEST_CSR_COUNT];

uint32_t difftestCount;
uint32_t memAddr = 0;
int memLen = 0;

bool skip = false;

void difftest_memcpy(uint32_t addr, uint32_t *buf, size_t n, bool direction) {
    nemu_difftest_memcpy(addr, buf, n, direction);
}

void difftest_init() {
    nemu_difftest_init(0);
    uint32_t initRegs[DIFFTEST_COMMON_REG_COUNT];
    for (int i = 0; i < DIFFTEST_COMMON_REG_COUNT; i++) {
        initRegs[i] = 0;
    }
    uint32_t pc = INST_START;
    nemu_difftest_pc(&pc, DIFFTEST_TO_REF);

    difftestCount = 0;
}

static void inline reg_cmp(int i, uint32_t dut) {
    if (refRegs[i] != dut) {
        panic(
            "Difftest failed.\nDifferent reg:\ni=%d, dut=%d(0x%08x), ref=%d(0x%08x)\ndut.pc=0x%08x inst=0x%08x",
            i, dut, dut, refRegs[i], refRegs[i], cpu.pc, cpu.inst
        );
    }
}

void difftest_regs() {
    nemu_difftest_regcpy(refRegs, DIFFTEST_TO_DUT);
    for (int i = 0; i < DIFFTEST_COMMON_REG_COUNT; i++) {
        reg_cmp(i, cpu.gpr[i]);
    }
}

#define csr_cmp(i, name) \
    Assert(refCSR[i] == cpu.name , "Difftest FAILED\nDifferent CSR: %s ref=" FMT_WORD \
    ", dut=" FMT_WORD " at pc=" FMT_WORD "(inst=" FMT_WORD ")", \
    #name, refCSR[i], cpu.name, cpu.pc, cpu.inst);

void difftest_csr() {
    nemu_difftest_csrcpy(refCSR, DIFFTEST_TO_DUT);
    csr_cmp(0, mcause);
    csr_cmp(1, mepc);
    csr_cmp(2, mscratch);
    csr_cmp(3, mstatus);
    csr_cmp(4, mtvec);
    csr_cmp(5, satp)
}

void difftest_write_mem(uint32_t addr, int len) {
    memAddr = addr;
    memLen = len;
}

void difftest_mem() {
    if (memLen != 0)
    {
        uint32_t ref = 0;
        nemu_difftest_memcpy(memAddr, &ref, memLen, DIFFTEST_TO_DUT);
        uint32_t dut = mem_read(memAddr, memLen);
        if (ref != dut) {
            panic("Difftest FAILED.\nDifferent memory: [0x%08x] dut=0x%08x, ref=0x%08x\ndut.pc=0x%08x(inst=0x%08x)\n", memAddr, dut, ref, cpu.pc, cpu.inst);
        } else {
            // Log("Difftest mem");
        }
        memAddr = 0;
        memLen = 0;
    }
}

void difftest_pc() {
    if (skip) {
        nemu_difftest_pc(&cpu.pc, DIFFTEST_TO_REF);
        skip = false;
    } else {
        uint32_t refPC;
        nemu_difftest_pc(&refPC, DIFFTEST_TO_DUT);
        if (refPC != cpu.pc) {
            extern uint32_t lastPC;
            extern uint32_t lastInst;
            panic("Difftest FAILED.\ndut.pc=0x%08x, ref.pc=0x%08x\nlastPC=" FMT_WORD "(inst=" FMT_WORD ")", cpu.pc, refPC, lastPC, lastInst);
        }
    }
}

void difftest_step() {
    bool nemu_skip;
    // nemu_difftest_skip(&nemu_skip, DIFFTEST_TO_DUT);
    if (skip || nemu_skip) {
        // nemu_difftest_regcpy(cpu.gpr, DIFFTEST_TO_REF);
        // nemu_difftest_pc(&cpu.pc, DIFFTEST_TO_REF);
        skip = false;
        nemu_skip = false;
        // nemu_difftest_skip(&nemu_skip, DIFFTEST_TO_REF);
    } else {
        nemu_difftest_exec(1);
        difftest_regs();
        difftest_csr();
        difftest_mem();
        difftest_pc();
    }
    difftestCount++;
}

void difftest_set_skip() {
    skip = true;
}

void difftest_end() {
    Log(ANSI_FG_GREEN "Difftest PASS. count=%d", difftestCount);
}

#else
void difftest_memcpy(uint32_t addr, uint32_t *buf, size_t n, bool direction) {}
void difftest_init(){}
void difftest_pc(){}
void difftest_regs(){}
void difftest_step(){}
void difftest_end(){}
void difftest_set_skip(){}

#endif
