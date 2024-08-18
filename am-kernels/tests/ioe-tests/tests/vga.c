#include "am.h"

#define VGA_BASE 0x21000000
#define VGA_ROW_COUNT 480
#define VGA_COL_COUNT 640

int main() {
    int cnt = VGA_ROW_COUNT * VGA_COL_COUNT;
    volatile uint8_t *buffer = (volatile uint8_t *)VGA_BASE;
    // for (int i = 0; i < VGA_COL_COUNT * 100; i++) {
    //     buffer[i * 3 + 0] = 0x00;
    //     buffer[i * 3 + 1] = 0xff;
    //     buffer[i * 3 + 2] = 0xff;
    // }
    for (int i = 0; i < cnt / 2; i++) {
        buffer[i * 3 + 0] = 0x00; // r
        buffer[i * 3 + 1] = 0xff; // g
        buffer[i * 3 + 2] = 0xff; // b
    }
    for (int i = cnt / 2; i < cnt; i++) {
        buffer[i * 3 + 0] = 0xff; // r
        buffer[i * 3 + 1] = 0xff; // g
        buffer[i * 3 + 2] = 0xff; // b
    }
    return 0;
}
