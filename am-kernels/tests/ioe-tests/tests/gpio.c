#include "am.h"
#include "klib.h"

#define GPIO_BASE 0x10002000
#define LED_REG *(volatile uint32_t *)(GPIO_BASE + 0)
#define SEG_REG *(volatile uint32_t *)(GPIO_BASE + 8)

int main() {
    printf("start\n");
    uint32_t i = 0;
    while (1)
    {
        SEG_REG = i;
        LED_REG = 1 << (i & 0xf);
        for (volatile int j = 0; j < 2000; j++);
        i++;
    }
    return 0;
}