#include "am.h"
#include "klib.h"

#define GPIO_BASE 0x10002000
#define SWI_REG *(volatile uint32_t *)(GPIO_BASE + 8)

int main() {
    uint32_t last = SWI_REG;
    while (1) {
        uint32_t c = SWI_REG;
        if (last != c) {
            last = c;
            printf("change to %x\n", c);
        }
    }
    return 0;
}
