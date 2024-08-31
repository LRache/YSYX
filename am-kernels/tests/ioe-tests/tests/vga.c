#include "am.h"

#define VGA_BASE 0x21000000
#define VGA_HEIGHT 480
#define VGA_WIDTH 640

int main() {
    // uint32_t buffer[10000];
    // for (int i = 0 ;i < 10000; i++) {
    //     buffer[i] = 0xffffff;
    // }
    uint32_t color = 0;
    for (int y = 0; y < VGA_HEIGHT; y++) {
        for (int x = 0; x < VGA_WIDTH; x++) {
            AM_GPU_FBDRAW_T ctl;
            ctl.h = 1;
            ctl.w = 1;
            ctl.x = x;
            ctl.y = y;
            ctl.pixels = &color;
            ioe_write(AM_GPU_FBDRAW, &ctl);
            color = (color + 1) % 0x00ffffff;
        }
    }
    while (1);    
    return 0;
}
