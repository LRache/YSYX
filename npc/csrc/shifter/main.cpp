#include <stdio.h>
#include <sys/time.h>
#include <verilated.h>
#include <nvboard.h>
#include "obj/Vtop.h"

#define INIT_V 0b10101010

long get_sec() {
    struct timeval t;
    gettimeofday(&t, NULL);
    return t.tv_sec;
}

int main() {
    Vtop top;
    nvboard_init();
    uint64_t t = 0xffffffffffffffff;
    nvboard_bind_pin(
        &top.seg, 14,
        SEG1G, SEG1F, SEG1E, SEG1D, SEG1C, SEG1B, SEG1A,
        SEG0G, SEG0F, SEG0E, SEG0D, SEG0C, SEG0B, SEG0A
    );
    nvboard_bind_pin(
        &t, 48,
        SEG2A, SEG2B, SEG2C, SEG2D, SEG2E, SEG2F, SEG2G, DEC2P,
        SEG3A, SEG3B, SEG3C, SEG3D, SEG3E, SEG3F, SEG3G, DEC3P,
        SEG4A, SEG4B, SEG4C, SEG4D, SEG4E, SEG4F, SEG4G, DEC4P,
        SEG5A, SEG5B, SEG5C, SEG5D, SEG5E, SEG5F, SEG5G, DEC5P,
        SEG6A, SEG6B, SEG6C, SEG6D, SEG6E, SEG6F, SEG6G, DEC6P,
        SEG7A, SEG7B, SEG7C, SEG7D, SEG7E, SEG7F, SEG7G, DEC7P
    );

    while (true) {
        top.clk = 0; top.eval();
        top.clk = 1; top.eval();
        long start = get_sec();
        while (get_sec() - start < 1) nvboard_update();
    }
    nvboard_quit();
    return 0;
}
