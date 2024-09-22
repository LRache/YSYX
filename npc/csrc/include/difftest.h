#ifndef __DIFFTEST_H__
#define __DIFFTEST_H__

#include "common.h"
#include <stddef.h>

#define DIFFTEST_COMMON_REG_COUNT 32
#define DIFFTEST_CSR_COUNT 6

enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

extern "C" {
    void nemu_difftest_memcpy(uint32_t addr, uint32_t *buf, size_t n, bool direction);
    void nemu_difftest_skip(bool *skip, bool directiob);
    void nemu_difftest_regcpy(uint32_t *reg, bool direction);
    void nemu_difftest_csrcpy(uint32_t *reg, bool direction);
    void nemu_difftest_pc(uint32_t *pc, bool direction);
    void nemu_difftest_exec(uint64_t n);
    void nemu_difftest_raise_intr(word_t NO);
    void nemu_difftest_init(int port);
    void nemu_difftest_statistic();
}

namespace difftest {
    enum { TO_DUT, TO_REF };
    
    void memcpy(uint32_t addr, uint32_t *buf, size_t n, bool direction);

    void write_mem(addr_t addr, int len);

    void init();
    void pc();
    void regs();
    void csr();
    void mem();
    void end();
    void set_skip();

    void step();
}


#endif
