#include <fstream>
#include <cstring>
#include <iostream>

#include "config.h"
#include "debug.h"
#include "memory.h"
#include "difftest.h"

#ifdef HAS_ROM

uint8_t rom[ROM_SIZE];

static inline void valid(addr_t addr) {
    if (addr < ROM_BASE || addr >= ROM_BASE + ROM_SIZE) {
        panic("ROM: addr=" FMT_WORD " out of range.", addr);
    }
}

extern "C" void mrom_read(addr_t addr, word_t *data) {
    valid(addr);
    addr = addr & ~0x3;
    *data = *(word_t *)(rom + addr - ROM_BASE);
    // Log("MROM read [" FMT_WORD "]", addr);
}

void set_rom(addr_t addr, word_t data) {
    *(word_t *)(rom + addr - ROM_BASE) = data;
}

void load_img_to_rom_from_file(const std::string &path) {
    if (path.empty()) return ;

    std::ifstream f;
    f.open(path, std::ios::binary);
    addr_t addr = ROM_BASE;
    while (!f.eof()) {
        uint32_t buf;
        f.read((char *)&buf, 4);
        set_rom(addr, buf);
        addr += 4;
        assert(addr <= ROM_BASE + ROM_SIZE);
    }
    difftest::memcpy(ROM_BASE, (uint32_t *)rom, addr - ROM_BASE, difftest::TO_REF);
    std::cout << "Load img to rom from file " << path << std::endl;
}

void load_img_to_rom_from_mem(const uint32_t *img, size_t length) {
    std::memcpy(rom, img, length);
    difftest::memcpy(ROM_BASE, (uint32_t *)img, length, difftest::TO_REF);
}

#else

extern "C" void mrom_read(addr_t addr, word_t *data) { *data = 0; }
void set_rom(addr_t addr, word_t data) {}
void load_img_to_rom_from_file(const std::string &path) {}
void load_img_to_rom_from_mem(const uint32_t *img, size_t length) {}

#endif

