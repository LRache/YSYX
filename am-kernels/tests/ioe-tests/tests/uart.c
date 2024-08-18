#include "klib.h"
#include "klib-macros.h"

int main() {
    printf("Hello World!\nfrom HCPU\n");
    while(1) {
        char c = io_read(AM_UART_RX).data;
        if (c != 0xff) {
            putch(c);
        }
    }
    return 0;
}
