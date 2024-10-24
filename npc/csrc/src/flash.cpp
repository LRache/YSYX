#include <cstddef>
#include <fstream>
#include <iostream>

#include "config.h"
#include "common.h"
#include "memory.h"
#include "debug.h"
#include "difftest.h"
#include "hdb.h"

#ifdef HAS_FLASH

uint8_t flash[FLASH_SIZE];

static inline uint32_t reserve(uint32_t d) {
    uint32_t t = 0;
    t |= (d & 0x000000ff) << 24;
    t |= (d & 0x0000ff00) <<  8;
    t |= (d & 0x00ff0000) >>  8;
    t |= (d & 0xff000000) >> 24;
    return t;
}

extern "C" void flash_read(addr_t addr, word_t *data) {
    word_t rdata = *(word_t *)(flash + (addr & ~0x3));
    *data = reserve(rdata);
    // Log("Read flash [" FMT_WORD "]=" FMT_WORD, FLASH_BASE + addr, *data);
}

void set_flash(addr_t addr, word_t data) {
    *(word_t *)(flash + addr - FLASH_BASE) = data;
}

void load_img_to_flash_from_file(const std::string &path) {
    if (path.empty()) return ;

    std::ifstream f;
    f.open(path, std::ios::binary);
    Assert(f.is_open(), "Can't open file: %s", path.c_str());
    
    addr_t addr = FLASH_BASE;
    while (!f.eof()) {
        uint32_t buf;
        f.read((char *)&buf, 4);
        set_flash(addr, buf);
        difftest::memcpy(addr, &buf, 4, difftest::TO_REF);
        addr += 4;
    }
    f.close();
    std::cout << "Load img to flash from file " << path << std::endl;
}

void load_img_to_flash_from_mem(const uint32_t *img, size_t length) {
    for (size_t i = 0; i < length; i++) {
        addr_t addr = FLASH_BASE + i * 4;
        set_flash(addr, img[i]);
        difftest::memcpy(addr, (uint32_t *)img + i, 4, difftest::TO_REF)
;    }
}

#else

extern "C" void flash_read(addr_t addr, word_t *data) {
    *data = 0;
}

void set_flash(addr_t addr, word_t data) {}

void load_img_to_flash_from_file(const std::string &path) {}

void load_img_to_flash_from_mem(const uint32_t *img, size_t length) {}

#endif

