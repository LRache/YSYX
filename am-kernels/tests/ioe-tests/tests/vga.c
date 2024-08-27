#include "am.h"

#define VGA_BASE 0x21000000
#define VGA_HEIGHT 480
#define VGA_WIDTH 640

int main() {
    // int cnt = VGA_HEIGHT * VGA_WIDTH;
    // volatile uint32_t *buffer = (volatile uint32_t *)VGA_BASE;
    // for (int i = 0; i < cnt / 2; i++) {
    //     buffer[i] = 0x00ffff00;
    // }
    // for (int i = cnt / 2; i < cnt; i++) {
    //     buffer[i] = 0x00ffffff;
    // }
    uint32_t buffer[10000];
    for (int i = 0 ;i < 10000; i++) {
        buffer[i] = 0xffffff;
    }
    AM_GPU_FBDRAW_T ctl;
    ctl.h = 100;
    ctl.w = 100;
    ctl.x = 200;
    ctl.y = 100;
    ctl.pixels = buffer;
    ioe_write(AM_GPU_FBDRAW, &ctl);
    // while (1);    
    return 0;
}
