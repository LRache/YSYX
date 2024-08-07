#include "config.h"
#include "common.h"
#include "memory.h"
#include "debug.h"

#ifdef HAS_FLASH

uint8_t flash[FLASH_SIZE];

extern "C" void flash_read(addr_t addr, word_t *data) {
    // Log("Read flash [" FMT_WORD "]", addr);
    *data = *(word_t *)(flash + addr);
}

void set_flash(addr_t addr, word_t data) {
    *(word_t *)(flash + addr - FLASH_BASE) = data;
    Log("Set flash");
}

#else

extern "C" void flash_read(addr_t addr, word_t *data) {
    *data = 0;
}

#endif

