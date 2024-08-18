#include "am.h"
#include "klib.h"

#define PSRAM_BASE 0x80000000
#define PSRAM_SIZE 0x400000

#define SDRAM_BASE 0xa0000000
#define SDRAM_SIZE 0x300000

#define MEM_BASE PSRAM_BASE
#define MEM_SIZE 0x1000

int main() {
    puts("Start  8-bit psram mem test\n");
    for (uint32_t addr = MEM_BASE; addr < MEM_BASE + MEM_SIZE; addr++) {
        *(volatile uint8_t *) addr = addr & 0xff;
    }
    for (uint32_t addr = MEM_BASE; addr < MEM_BASE + MEM_SIZE; addr++) {
        if ((*(volatile uint8_t *) addr) != (addr & 0xff)) {
            halt(1);
        }
    }
    puts("Start 16-bit psram mem test\n");
    for (uint32_t addr = MEM_BASE; addr < MEM_BASE + MEM_SIZE; addr+=2) {
        *(volatile uint16_t *) addr = addr & 0xffff;
    }
    for (uint32_t addr = MEM_BASE; addr < MEM_BASE + MEM_SIZE; addr+=2) {
        if ((*(volatile uint16_t *) addr) != (addr & 0xffff)) {
            halt(1);
        }
    }
    puts("Start 32-bit psram mem test\n");
    for (uint32_t addr = MEM_BASE; addr < MEM_BASE + MEM_SIZE; addr+=4) {
        *(volatile uint32_t *) addr = addr;
    }
    for (uint32_t addr = MEM_BASE; addr < MEM_BASE + MEM_SIZE; addr+=4) {
        if ((*(volatile uint32_t *) addr) != addr) {
            halt(1);
        }
    }
    return 0;
}
