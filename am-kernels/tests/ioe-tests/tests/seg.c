#include "am.h"
#include "klib.h"

#define GPIO_BASE 0x10002000
#define SEG_REG *(volatile uint32_t *)(GPIO_BASE + 8)

int main() {
    printf("start\n");
    while (1)
    {
        for (int i = 0; i < 16; i++) {
            SEG_REG = i + (i << 8);
            for (volatile int j = 0; j < 2000; j++);
        }
    }
    return 0;
}