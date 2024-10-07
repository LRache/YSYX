#include "config.h"
#include "memory.h"
#include "debug.h"

#ifdef HAS_PSRAM

uint8_t psram[PSRAM_SIZE];

extern "C" void psram_read(addr_t addr, uint8_t *data, int count) {
    addr_t raddr = (addr & ~0x3) + count / 2;
    *data = psram[raddr];
    if (count % 2 == 0) {
        *data >>= 4;
    } else {
        *data &= 0b00001111;
    }
    // Log("PSRAM read [" FMT_WORD "]=" FMT_WORD " (%d)", raddr + PSRAM_BASE, *data, count);
}

extern "C" void psram_write(addr_t addr, uint8_t data, int count) {
    addr_t waddr = addr + count / 2;
    if (count % 2 == 0) {
        psram[waddr] = (psram[waddr] & 0b00001111) | ((data & 0b00001111) << 4);
    } else {
        psram[waddr] = (psram[waddr] & 0b11110000) | (data & 0b00001111);
    }
    // Log("PSRAM write %02d [" FMT_WORD "]=" FMT_WORD " (%d)", c, addr, ((uint32_t *)psram)[addr / 4], count);
}

#else

extern "C" void psram_read(addr_t addr, word_t *data) {}
extern "C" void psram_write(addr_t addr, uint8_t data, int count) {}

#endif
