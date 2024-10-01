#include <fstream>
#include <iostream>

#include "memory.h"
#include "debug.h"
#include "difftest.h"
#include "hdb.h"
#include "config.h"
#include "utils.hpp"

#ifdef HAS_MEM

uint8_t mem[MEM_SIZE];

static inline void valid(addr_t addr, bool isRead) {
    if (addr < MEM_BASE or addr > MEM_BASE + MEM_SIZE) {
        panic("%s: addr=" FMT_WORD " is out of bound at " FMT_WORD " (inst=" FMT_WORD ").", isRead? "MemRead" : "MemWrite", addr, cpu.pc, cpu.inst);
    }
}

word_t mem_read(addr_t addr, int len) {
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

void mem_write(addr_t addr, word_t data, int len) {
    valid(addr,false);
    switch (len)
    {
        case 1: *(uint8_t  *)(mem + addr - MEM_BASE) = (uint8_t )data; break;
        case 2: *(uint16_t *)(mem + addr - MEM_BASE) = (uint16_t)data; break;
        case 4: *(uint32_t *)(mem + addr - MEM_BASE) = (uint32_t)data; break;
        default: panic("Invalid len=%d", len);
    }
}

void load_img_to_mem_from_file(const std::string &path) {
    if (path.empty()) return ;
    std::ifstream f;
    f.open(path, std::ios::binary);
    addr_t addr = 0;
    while (!f.eof()) {
        uint32_t buf;
        f.read((char *)buf, 4);
        *(uint32_t *)(mem + addr) = buf;
        addr += 4;
    }
    difftest::memcpy(MEM_BASE, (uint32_t *)mem, addr, difftest::TO_REF);
    f.close();

    std::cout << "Load img to mem from file " << path << std::endl;
}

void load_img_to_mem_from_mem(const uint32_t *img, size_t length) {
    int i = 0;
    for (addr_t addr = 0; addr < length; addr+=4) {
        *(uint32_t *)(mem + addr) = *(img + i);
        i++;
    }
}

bool mmio_read(addr_t addr, word_t *data) {
    return false;
}

bool mmio_write(addr_t addr, word_t data, int len) {
    if (addr == 0xa00003f8) {
        putchar(data);
        fflush(stdout);
        difftest::set_skip();
        return true;
    }
    return false;
}

void sim_pmem_read(addr_t addr, word_t *data, int size) {
    // ERROR!
    if (mmio_read(addr, data)) return;
    *data = mem_read(addr, size);
}

void sim_pmem_write(addr_t addr, word_t data, uint8_t mask) {
    // ERROR !
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
    difftest::write_mem(addr, len);
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
void load_img_to_mem_from_file(const std::string &path) {}
void load_img_to_mem_from_mem(const uint32_t *img, size_t length) {}

#endif
