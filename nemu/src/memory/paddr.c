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

#include "macro.h"
#include <memory/host.h>
#include <memory/paddr.h>
#include <device/mmio.h>
#include <isa.h>

#if   defined(CONFIG_PMEM_MALLOC)
static uint8_t *pmem = NULL;
#else // CONFIG_PMEM_GARRAY
// static uint8_t pmem[CONFIG_MSIZE] PG_ALIGN = {};
static uint8_t mrom[MROM_SIZE] PG_ALIGN = {};
static uint8_t sram[SRAM_SIZE] PG_ALIGN = {};
static uint8_t flash[FLASH_SIZE] PG_ALIGN = {};
static uint8_t psram[PSRAM_SIZE] PG_ALIGN = {};
static uint8_t sdram[SDRAM_SIZE] PG_ALIGN = {};
#endif

void set_difftest_skip(bool skip);

// uint8_t* guest_to_host(paddr_t paddr) { return pmem + paddr - CONFIG_MBASE; }
// paddr_t host_to_guest(uint8_t *haddr) { return haddr - pmem + CONFIG_MBASE; }

// static word_t pmem_read(paddr_t addr, int len) {
//   word_t ret = host_read(guest_to_host(addr), len);
//   trace_mem(addr, len, MEM_READ);
//   return ret;
// }

// static void pmem_write(paddr_t addr, int len, word_t data) {
//   host_write(guest_to_host(addr), len, data);
//   trace_mem(addr, len, MEM_WRITE);
// }

static void out_of_bound(paddr_t addr) {
  // panic("address = " FMT_PADDR " is out of bound of pmem [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
  //     addr, PMEM_LEFT, PMEM_RIGHT, cpu.pc);
  panic("address = " FMT_PADDR " is out of bound of pmem at pc = " FMT_WORD,
      addr, cpu.pc);
}

void init_mem() {
#if   defined(CONFIG_PMEM_MALLOC)
  pmem = malloc(CONFIG_MSIZE);
  assert(pmem);
#endif
  // IFDEF(CONFIG_MEM_RANDOM, memset(pmem, rand(), CONFIG_MSIZE));
  // Log("physical memory area [" FMT_PADDR ", " FMT_PADDR "]", PMEM_LEFT, PMEM_RIGHT);
}

extern bool nemu_difftest_skip;

word_t paddr_read(paddr_t addr, int len) {
  // if (likely(in_pmem(addr))) return pmem_read(addr, len);
  // IFDEF(CONFIG_DEVICE, return mmio_read(addr, len));
  if (likely(in_mrom(addr))) return host_read(mrom + addr - MROM_BASE, len);
  if (likely(in_sram(addr))) return host_read(sram + addr - SRAM_BASE, len);
  if (likely(in_flash(addr))) return host_read(flash + addr - FLASH_BASE, len);
  if (likely(in_psram(addr))) return host_read(psram + addr - PSRAM_BASE, len);
  if (likely(in_sdram(addr))) return host_read(sdram + addr - SDRAM_BASE, len);
  if (likely(in_uart(addr)))  {set_difftest_skip(true); return 0;}
  if (likely(in_clint(addr))) {set_difftest_skip(true); return 0;}
  out_of_bound(addr);
  return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) {
  // if (likely(in_pmem(addr))) { pmem_write(addr, len, data); return; }
  // IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
  if (likely(in_mrom(addr))) { host_write(mrom + addr - MROM_BASE, len, data); return ;}
  if (likely(in_sram(addr))) { host_write(sram + addr - SRAM_BASE, len, data); return ;}
  if (likely(in_flash(addr))) { host_write(flash + addr - FLASH_BASE, len, data); return ;}
  if (likely(in_psram(addr))) { host_write(psram + addr - PSRAM_BASE, len, data); return ;}
  if (likely(in_sdram(addr))) {host_write(sdram + addr - SDRAM_BASE, len, data); return ;}
  if (likely(in_uart(addr))) { set_difftest_skip(true); return ;}
  // out_of_bound(addr);
}
