#include "klib.h"

#define CLINT_BASE 0x02000000
#define CLINT_LOW  *(volatile uint32_t *)(CLINT_BASE + 0x0)
#define CLINT_HIGH *(volatile uint32_t *)(CLINT_BASE + 0x4)

int main() {
    printf("Hello World!\n");
    printf("%u\n", CLINT_LOW );
    printf("%u\n", CLINT_HIGH);
    return 0;
}
