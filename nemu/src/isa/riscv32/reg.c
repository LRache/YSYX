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
#include "local-include/reg.h"
#include "common.h"

const char *regs[] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

void isa_reg_display() {
  int i = 0;
  for (int j = 0; j < 4; j++)
  {
    for (int k = 0; k < 8; k++)
    {
      printf("%s=0x%08x\t", regs[i], cpu.gpr[i]);
      i++;
    }
    printf("\n");
  }
  printf("mcause=0x%x\n", cpu.mcause);
}

word_t isa_reg_str2val(const char *s, bool *success) {
  if (s[0] == 'p' && s[1] == 'c') return cpu.pc;
  for (int i = 0; i < 32; i++) {
    if (s[0] == regs[i][0] && s[1] == regs[i][1]) {
      return cpu.gpr[i];
    }
  }
  *success = false;
  return 0;
}

word_t* get_csr(int idx) {
  switch (idx)
  {
    case 0x180: return &cpu.satp;
    case 0x300: return &cpu.mstatus;
    case 0x305: return &cpu.mtvec;
    case 0x340: return &cpu.mscratch;
    case 0x341: return &cpu.mepc;
    case 0x342: return &cpu.mcause;
  }
  Log("Unknown csr id %x", idx);
  panic();
  return NULL;
}
