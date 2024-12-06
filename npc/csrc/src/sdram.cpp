#include <stdint.h>
#include "config.h"
#include "memory.h"
#include "debug.h"

#ifdef HAS_SDRAM

uint16_t sdram[4][8192][512];

extern "C" void sdram_read (int bank, int row, int column, uint16_t *data) {
    *data = sdram[bank][row][column];
    #ifdef DEBUG_LOG
    Log("SDRAM Read [%d][%d][%d]=0x%04x" , bank, row, column, *data);
    #endif
}

extern "C" void sdram_write(int bank, int row, int column, uint16_t data, uint8_t _mask) {
    #ifdef DEBUG_LOG
    Log("SDRAM Write [%d][%d][%d]=0x%04x mask=%d", bank, row, column, data, _mask);
    #endif
    uint16_t mask = 0;
    switch (_mask) 
    {
        case 0b10: mask = 0x00ff; break;
        case 0b01: mask = 0xff00; break;
        case 0b00: mask = 0xffff; break;
        case 0b11: mask = 0x0000; break;
        // default: panic("Invalid mask=%x", _mask); break;
    }
    sdram[bank][row][column] &= ~mask;
    sdram[bank][row][column] |= data & mask;
}

#else

extern "C" void sdram_read(int bank, int row, int column, uint16_t *data) {}
extern "C" void sdram_write(int bank, int row, int column, uint16_t data, uint8_t _mask) {}

#endif
