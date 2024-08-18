#include "am.h"
#include "klib.h"

#define PS2_BASE 0x10011000
#define PS2_REG  (*(volatile uint8_t *)(PS2_BASE + 0))

static inline uint8_t scan() {
  uint8_t key = PS2_REG;
  while (key == 0) key = PS2_REG;
  return key;
}

int main() {
    puts("start\n");
    while (1) {
        // volatile uint8_t key = PS2_REG;
        // if (key == 0) {
        // } else {
        //     int keycode = AM_KEY_NONE;
        //     bool keydown = true;
        //     if (key == 0xf0) {
        //         for (volatile int i = 0; i < 2; i++);
        //         key = scan();
        //         keydown = false;
        //     }
        //     switch (key) {
        //         case 0x1c: keycode = AM_KEY_A;
        //     }
        //     if (keydown) {
        //         printf("DOWN 0x%x\n", keycode);
        //     } else {
        //         printf("UP 0x%x\n", keycode);
        //     }
        // }
        AM_INPUT_KEYBRD_T kbd;
        ioe_read(AM_INPUT_KEYBRD, &kbd);
        if (kbd.keycode != AM_KEY_NONE) {
            if (kbd.keydown) {
                puts("DOWN\n");
            } else {
                puts("UP\n");
            }
        }
    }
    return 0;
}
