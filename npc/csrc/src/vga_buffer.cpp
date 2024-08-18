#include "config.h"
#include "common.h"
#include "debug.h"

#ifdef HAS_VGA

uint8_t vgaBuffer[VGA_BUFFER_SIZE];

extern "C" void vga_buffer_write(addr_t addr, word_t data, int mask) {
    // Log(FMT_WORD " %d", addr, mask);
    uint32_t wmask = 0;
    for (int i = 0; i < 4; i++) {
        if (mask & (1 << i)) {
            wmask |= 0xff << (i * 8);
        }
    }
    addr = addr & (~0x3) - VGA_BASE;
    *(uint32_t *)(vgaBuffer + addr) &= ~wmask;
    // Log(FMT_WORD, wmask);
    *(uint32_t *)(vgaBuffer + addr) |= data & wmask;
}

extern "C" void vga_buffer_read(uint32_t x, uint32_t y, word_t *data) {
    assert(x >= 0 && x < VGA_COL_COUNT);
    assert(y >= 0 && y < VGA_ROW_COUNT);
    uint32_t i = (y * VGA_COL_COUNT + x) * 3;
    *data = ((vgaBuffer[i+2] & 0xff) << 16) | ((vgaBuffer[i+1] & 0xff) << 8) | ((vgaBuffer[i] & 0xff));
}

#else

extern "C" void write_vga_buffer(addr_t addr, word_t data, int mask){}

#endif
