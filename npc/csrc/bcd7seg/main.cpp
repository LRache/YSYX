#include <stdio.h>
#include <verilated.h>
#include <sys/time.h>
#include "nvboard.h"
#include "obj/Vbcd7seg.h"

Vbcd7seg *top = NULL;

long get_sec() {
    struct timeval t;
    gettimeofday(&t, NULL);
    return t.tv_sec;
}

int main() {
    top = new Vbcd7seg;
    nvboard_init();
    nvboard_bind_pin(&top->h, 7, SEG0G, SEG0F, SEG0E, SEG0D, SEG0C, SEG0B, SEG0A);
    for (uint8_t i = 0; i < 16; i++) {
        printf("i=%d\n", i);
        top->b = i;
        top->eval();
        long start = get_sec();
        while(get_sec() - start <= 1) {
            top->eval();
            nvboard_update();
        }
    }
    nvboard_quit();
    return 0;
}
