#include "memory.h"
#include "debug.h"
#include "difftest.h"
#include "hdb.h"
#include "config.h"
#include "utils.h"

#ifdef HAS_MEM

uint8_t mem[MEM_SIZE];

void valid(word_t addr, bool isRead) {
    if (addr < MEM_BASE or addr > MEM_BASE + MEM_SIZE) {
        panic("%s: addr=" FMT_WORD " is out of bound at " FMT_WORD " (inst=" FMT_WORD ").", isRead? "MemRead" : "MemWrite", addr, cpu.pc, cpu.inst);
    }
}

word_t mem_read(word_t addr, int len) {
    valid(addr, true);
    if (addr < MEM_BASE or addr > MEM_BASE + MEM_SIZE) return 0;
    word_t rdata;
    switch (len)
    {
        case 1: rdata = *(uint8_t  *)(mem + addr - MEM_BASE); break;
        case 2: rdata = *(uint16_t *)(mem + addr - MEM_BASE); break;
        case 4: rdata = *(uint32_t *)(mem + addr - MEM_BASE); break;
        default: panic("Invalid len=%d", len);
    }
    return rdata;
}

void mem_write(word_t addr, word_t data, int len) {
    valid(addr,false);
    switch (len)
    {
        case 1: *(uint8_t  *)(mem + addr - MEM_BASE) = (uint8_t )data; break;
        case 2: *(uint16_t *)(mem + addr - MEM_BASE) = (uint16_t)data; break;
        case 4: *(uint32_t *)(mem + addr - MEM_BASE) = (uint32_t)data; break;
        default: panic("Invalid len=%d", len);
    }
    // Log("Memory write: addr=0x%08x, data=%d(0x%08x), len=%d", addr, data, data, len);
    // Log("Data in mem: 0x%08x", *(uint32_t *)(mem + addr - MEM_BASE));
}

bool mmio_read(addr_t addr, word_t *data) {
    return false;
}

bool mmio_write(addr_t addr, word_t data, int len) {
    if (addr == 0xa00003f8) {
        putchar(data);
        fflush(stdout);
        difftest_set_skip();
        return true;
    }
    return false;
}

void sim_pmem_read(addr_t addr, word_t *data, int size) {
    // Log("PMEM Read: [" FMT_WORD "]", addr);
    if (mmio_read(addr, data)) return;
    *data = mem_read(addr, size);
    // Log("PMEM Read: [" FMT_WORD "] = " FMT_WORD, addr, *data);
}

void sim_pmem_write(addr_t addr, word_t data, uint8_t mask) {
    int len;
    switch (mask)
    {
        case 0b0001: len = 1; break;
        case 0b0011: len = 2; break;
        case 0b1111: len = 4; break;
        default: panic("Invalid mask=%d", mask);
    }
    if (mmio_write(addr, data, len)) return ;
    mem_write(addr, data, len);
    // Log("PMEM Write: [" FMT_WORD "] = " FMT_WORD, addr, data);
    #ifdef DIFFTEST
    difftest_write_mem(addr, len);
    #endif
}

extern "C" {
    void pmem_read (addr_t addr, word_t *data, int size) {
        sim_pmem_read(addr, data, size);
    }
    void pmem_write(addr_t addr, word_t data, uint8_t mask) {
        sim_pmem_write(addr, data, mask);
    }
}

#else

extern "C" {
    void pmem_read (addr_t addr, word_t *data, int size) {}
    void pmem_write(addr_t addr, word_t data, uint8_t mask) {}
}

word_t  mem_read (addr_t addr, int len) {return 0;}
void    mem_write(addr_t addr, word_t data, int len) {}

#endif
