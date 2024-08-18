#include "am.h"
#include "klib.h"

#define GPIO_BASE 0x10002000
#define LED_REG *(volatile uint32_t *)(GPIO_BASE + 0)

int main() {
    printf("start\n");
    while (1)
    {
        for (int i = 0; i < 16; i++) {
            LED_REG = 1 << i;
            for (volatile int j = 0; j < 2000; j++);
        }
    }
    return 0;
}