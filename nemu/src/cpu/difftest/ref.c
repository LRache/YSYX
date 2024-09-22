/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <isa.h>
#include <cpu/cpu.h>
#include <difftest-def.h>
#include <memory/paddr.h>

extern void execute(uint64_t n);

bool difftest_skip = false;

void set_difftest_skip(bool skip) {
  difftest_skip = skip;
}

__EXPORT void nemu_difftest_memcpy(paddr_t addr, void *buf, size_t n, bool direction) {
  if (direction == DIFFTEST_TO_DUT) {
    uint8_t *dest = buf;
    for (int i = 0; i < n; i++) {
      dest[i] = (uint8_t) paddr_read(addr + i, 1);
    }
  } else {
    uint8_t *src = buf;
    for (int i = 0; i < n; i++) {
      paddr_write(addr+i, 1, src[i]);
    }
  }
}

__EXPORT void nemu_difftest_skip(bool *skip, bool direction) {
  if (direction == DIFFTEST_TO_DUT) {
    *skip = difftest_skip;
  } else {
    difftest_skip = *skip;
  }
}

__EXPORT void nemu_difftest_regcpy(void *reg, bool direction) {
  if (direction == DIFFTEST_TO_DUT) {
    uint32_t *dest = reg;
    for (int i = 0; i < 16; i++) {
      dest[i] = cpu.gpr[i];
    }
  } else {
    uint32_t *src = reg;
    for (int i = 0; i < 16; i++) {
      cpu.gpr[i] = src[i];
    }
  }
}

__EXPORT void nemu_difftest_csrcpy(void *reg, bool direction) {
  if (direction == DIFFTEST_TO_DUT) {
    uint32_t *dest = reg;
    dest[0] = cpu.mcause;
    dest[1] = cpu.mepc;
    dest[2] = cpu.mscratch;
    dest[3] = cpu.mstatus;
    dest[4] = cpu.mtvec;
    dest[5] = cpu.satp;
  } else {
    uint32_t *src = reg;
    cpu.mcause = src[0];
    cpu.mepc = src[1];
    cpu.mscratch = src[2];
    cpu.mstatus = src[3];
    cpu.mtvec = src[4];
    cpu.satp = src[5];
  }
}

__EXPORT void nemu_difftest_pc(uint32_t *pc, bool direction) {
  if (direction == DIFFTEST_TO_DUT) {
    *pc = cpu.refPC;
  } else {
    cpu.pc = *pc;
  }
}

__EXPORT void nemu_difftest_exec(uint64_t n) {
  execute(n);
}

__EXPORT void nemu_difftest_raise_intr(word_t NO) {
  assert(0);
}

void statistic();
__EXPORT void nemu_difftest_statistic() {
  statistic();
}

__EXPORT void nemu_difftest_init(int port) {
  void init_mem();
  init_mem();
  /* Perform ISA dependent initialization. */
  init_isa();
}
