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
#include <memory/vaddr.h>
#include <memory/paddr.h>

#define PTE_SIZE 4

paddr_t isa_mmu_translate(vaddr_t vaddr, int len, int type) {
  if (!(cpu.satp >> 31)) return vaddr;

  paddr_t paddr = MEM_RET_FAIL;
  uint32_t pgoff = vaddr & 0xfff;
  uint32_t vpn[2];
  vpn[0] = (vaddr >> 12) & 0x3ff;
  vpn[1] = (vaddr >> 22) & 0x3ff;
  uint32_t a = (cpu.satp & 0x3fffff) * PAGE_SIZE;
  uint32_t pdir = a;
  for (int i = 1; i >= 0; i--) {
    uint32_t pte = paddr_read(a + vpn[i] * PTE_SIZE, 4);
    uint32_t v = pte & 0x1;
    if (!v) {
      Log("Invalid PTE: PTE=0x%08x, i=%d, vaddr=0x%08x, pdir=0x%x, vpn1=0x%x, vpn0=0x%x, paddr=0x%x, type=%d", 
      pte, i, vaddr, pdir, vpn[1], vpn[0], a + vpn[i] * PTE_SIZE, type);
      //panic("Invalid PTE");
      return 1;
    }
    
    uint32_t ppn = pte >> 10;
    uint32_t r = (pte & 0x2) >> 1;
    uint32_t w = (pte & 0x4) >> 2;
    uint32_t x = (pte & 0x8) >> 3;
    
    if (r || w || x) {
      paddr = ppn * PAGE_SIZE + pgoff;
      break;
    }
    a = ppn * PAGE_SIZE;
  }
  return paddr;
}