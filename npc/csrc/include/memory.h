#ifndef __MEMORY_H__
#define __MEMORY_H__

#include <string>

#include "common.h"
#include "config.h"

extern "C" void pmem_read (addr_t addr, word_t *data, int size);
extern "C" void pmem_write(addr_t addr, word_t data, uint8_t mask);

word_t  mem_read (addr_t addr, int len);
void    mem_write(addr_t addr, word_t data, int len);

extern "C" void mrom_read(addr_t addr, word_t *data);
void set_rom(addr_t addr, word_t data);

extern "C" void flash_read(addr_t addr, word_t *data);
void set_flash(addr_t addr, word_t data);

extern "C" void psram_read(addr_t addr, uint8_t *data, int count);
extern "C" void psram_write(addr_t addr, uint8_t data, int count);

extern "C" void sdram_read(int bank, int row, int column, uint16_t *data);
extern "C" void sdram_write(int bank, int row, int column, uint16_t data, uint8_t _mask);

void load_img_to_mem_from_file(const std::string &path);
void load_img_to_mem_from_mem(const uint32_t *img, size_t length);

void load_img_to_rom_from_file(const std::string &path);
void load_img_to_rom_from_mem(const uint32_t *img, size_t length);

void load_img_to_flash_from_file(const std::string &path);
void load_img_to_flash_from_mem(const uint32_t *img, size_t length);

static inline bool in_flash(addr_t addr) {
    #ifdef HAS_FLASH
    return addr - FLASH_BASE < FLASH_SIZE;
    #else
    return false;
    #endif
}

static inline bool in_sram(addr_t addr) {
    #ifdef HAS_SRAM
    return addr - SRAM_BASE < SRAM_SIZE;
    #else
    return false;
    #endif
}

static inline bool in_psram(addr_t addr) {
    #ifdef HAS_PSRAM
    return addr - PSRAM_BASE < PSRAM_SIZE;
    #else
    return false;
    #endif
}

static inline bool in_sdram(addr_t addr) {
    #ifdef HAS_SDRAM
    return addr - SDRAM_BASE < SDRAM_SIZE;
    #else
    return false;
    #endif
}

static inline bool in_vga(addr_t addr) {
    #ifdef HAS_VGA
    return addr - VGA_BASE < VGA_SIZE;
    #else
    return false;
    #endif
}

static inline bool in_uart(addr_t addr) {
    return false;
}

#endif
