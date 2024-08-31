#include "am.h"
#include "klib.h"

#define VGA_BASE 0x21000000
#define VGA_HEIGHT 100
#define VGA_WIDTH 100

int main() {
    // uint32_t buffer[10000];
    // for (int i = 0 ;i < 10000; i++) {
    //     buffer[i] = 0xffffff;
    // }
    uint32_t buffer[VGA_WIDTH][VGA_HEIGHT];
    // uint32_t color = 0;
    for (int y = 0; y < VGA_HEIGHT; y++) {
        for (int x = 0; x < VGA_WIDTH; x++) {
            buffer[x][y] = 0x12;
            // color += 32;
        }
        printf("%d\n", y);
    }
    puts("start\n");
    AM_GPU_FBDRAW_T ctl;
    ctl.h = VGA_HEIGHT;
    ctl.w = VGA_WIDTH;
    ctl.x = 0;
    ctl.y = 0;
    ctl.pixels = buffer;
    ioe_write(AM_GPU_FBDRAW, &ctl);
    while (1);    
    return 0;
}
