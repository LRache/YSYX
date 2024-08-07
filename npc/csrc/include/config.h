#ifndef __CONFIG_H__
#define __CONFIG_H__

#define INST_START 0x20000000

// #define HAS_MEM
#define MEM_BASE 0x80000000
#define MEM_SIZE 0x8000000

#define HAS_ROM
#define ROM_BASE 0x20000000
#define ROM_SIZE 0x1000

#define HAS_FLASH
#define FLASH_BASE 0x30000000
#define FLASH_SIZE 0x10000000

// #define DIFFTEST

#endif
