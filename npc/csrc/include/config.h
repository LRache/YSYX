#ifndef __CONFIG_H__
#define __CONFIG_H__

// #define HAS_NVBOARD

// #define HAS_MEM
#define MEM_BASE 0x80000000
#define MEM_SIZE 0x8000000

#define HAS_SRAM
#define SRAM_BASE 0x0f000000
#define SRAM_SIZE (8 * 1024)

#define HAS_ROM
#define ROM_BASE 0x20000000
#define ROM_SIZE 0x1000

#define HAS_FLASH
#define FLASH_BASE 0x30000000
#define FLASH_SIZE 0x1000000

#define HAS_PSRAM
#define PSRAM_BASE 0x80000000
#define PSRAM_SIZE 0x400000

#define HAS_SDRAM
#define SDRAM_BASE 0xa0000000
#define SDRAM_SIZE (4 * 8192 * 512 * 2)

#define HAS_VGA
#define VGA_HEIGHT 480
#define VGA_WIDTH 640
#define VGA_BASE 0x21000000
#define VGA_BUFFER_SIZE (VGA_HEIGHT * VGA_WIDTH * 4)

#define INST_START FLASH_BASE

#define DIFFTEST
#define ITRACE

#endif
