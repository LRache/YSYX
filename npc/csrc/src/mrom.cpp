#include "config.h"
#include "debug.h"
#include "memory.h"

#ifdef HAS_ROM

uint8_t rom[ROM_SIZE];

static void valid(addr_t addr) {
    if (addr < ROM_BASE || addr >= ROM_BASE + ROM_SIZE) {
        panic("ROM: addr=" FMT_WORD " out of range.", addr);
    }
}

extern "C" void mrom_read(addr_t addr, word_t *data) {
    valid(addr);
    addr = addr & ~0x3;
    *data = *(word_t *)(rom + addr - ROM_BASE);
}

void set_rom(addr_t addr, word_t data) {
    *(word_t *)(rom + addr - ROM_BASE) = data;
}

#endif

