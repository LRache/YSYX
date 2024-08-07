#ifndef __MEMORY_H__
#define __MEMORY_H__

#include "common.h"

extern "C" void pmem_read (addr_t addr, word_t *data, int size);
extern "C" void pmem_write(addr_t addr, word_t data, uint8_t mask);

word_t  mem_read (addr_t addr, int len);
void    mem_write(addr_t addr, word_t data, int len);

extern "C" void mrom_read(addr_t addr, word_t *data);
void set_rom(addr_t addr, word_t data);

#endif
