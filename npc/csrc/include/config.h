#ifndef __CONFIG_H__
#define __CONFIG_H__

#define HAS_NVBOARD
// #define DIFFTEST
#define PERF
#define TRACE
#define CHECK_STEP_TIMEOUT

#ifdef TRACE
    #define ITRACE
    #define ICTRACE
    #define DTRACE
#endif

#define DEBUG_LOG

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
#define VGA_SIZE VGA_BUFFER_SIZE

#define UART_BASE 0x10000000
#define UART_SIZE 0x1000

#define INST_START FLASH_BASE

#define REAL_FREQ ((uint64_t)(856.498 * 1000000))

#include <string>

namespace config {
    extern bool hasNVBoard;
    extern bool hasDifftest;
    extern bool perf;

    extern bool statistic;
    extern std::string statisticOutputFileName;
    
    extern bool itrace;
    extern std::string itraceOutputFileName;
    extern bool ictrace;
    extern std::string ictraceOutputFileName;
    extern bool dtrace;
    extern std::string dtraceOutputFileName;
    extern bool zip;

    extern bool loadRom;
    extern std::string romImgFileName;
    extern bool loadFlash;
    extern std::string flashImgFileName;

    extern bool allowIllegalInstruction;
}

#endif
