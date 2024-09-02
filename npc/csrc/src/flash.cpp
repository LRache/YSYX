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

extern "C" void flash_read(addr_t addr, word_t *data) {
    word_t d = *(word_t *)(flash + (addr & ~0x3));
    word_t rdata = 0;
    rdata |= (d & 0x000000ff) << 24;
    rdata |= (d & 0x0000ff00) <<  8;
    rdata |= (d & 0x00ff0000) >>  8;
    rdata |= (d & 0xff000000) >> 24;
    *data = rdata;
    // Log("Read flash [" FMT_WORD "]=" FMT_WORD, addr, d);
}

void set_flash(addr_t addr, word_t data) {
    *(word_t *)(flash + addr - FLASH_BASE) = data;
}

void load_img_to_flash_from_file(const std::string &path) {
    if (path.empty()) return ;

    std::ifstream f;
    f.open(path, std::ios::binary);
    assert(f.is_open());
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
    for (int i = 0; i < length; i++) {
        addr_t addr = FLASH_BASE + i * 4;
        set_flash(addr, img[i]);
        difftest::memcpy(addr, (uint32_t *)img + i, 4, difftest::TO_REF)
;    }
}

#else

extern "C" void flash_read(addr_t addr, word_t *data) {
    *data = 0;
}

#endif

