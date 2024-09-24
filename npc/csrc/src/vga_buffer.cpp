#include "config.h"
#include "common.h"
#include "debug.h"

#ifdef HAS_VGA

uint8_t vgaBuffer[VGA_BUFFER_SIZE];

extern "C" void vga_buffer_write(addr_t addr, word_t data, int mask) {
    uint32_t wmask = 0;
    for (int i = 0; i < 4; i++) {
        if (mask & (1 << i)) {
            wmask |= 0xff << (i * 8);
        }
    }
    addr = (addr & (~0x3)) - VGA_BASE;
    *(uint32_t *)(vgaBuffer + addr) &= ~wmask;
    *(uint32_t *)(vgaBuffer + addr) |= data & wmask;
}

extern "C" void vga_buffer_read(uint32_t x, uint32_t y, word_t *data) {
    assert(x < VGA_WIDTH);
    assert(y < VGA_HEIGHT);
    uint32_t i = y * VGA_WIDTH + x;
    *data = ((uint32_t *)vgaBuffer)[i];
}

#else

extern "C" void write_vga_buffer(addr_t addr, word_t data, int mask){}

#endif
