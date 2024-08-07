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
}

void difftest_memcpy(uint32_t addr, uint32_t *buf, size_t n, bool direction);

void difftest_write_mem(uint32_t addr, int len);

void difftest_init();
void difftest_pc();
void difftest_regs();
void difftest_mem();
void difftest_step();
void difftest_end();

void difftest_set_skip();

#endif
