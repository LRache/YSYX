#include "am.h"

#define UART_BASE 0x10000000
#define UART_LCR (*(volatile uint8_t *)(UART_BASE + 3))
#define UART_THR (*(volatile uint8_t *)(UART_BASE + 0))
#define UART_RBR (*(volatile uint8_t *)(UART_BASE + 0))
#define UART_FCR (*(volatile uint8_t *)(UART_BASE + 2))
#define UART_LSR (*(volatile uint8_t *)(UART_BASE + 5))

#define UART_LSB (*(volatile uint8_t *)(UART_BASE + 0))
#define UART_MSB (*(volatile uint8_t *)(UART_BASE + 1))

// #define DIVISOR (uint16_t)(12000000 / (16 * 9600))
#define DIVISOR 1

void __am_uart_init() {
    UART_LCR |= 0x80;
    UART_LSB = DIVISOR & 0xff;
    UART_MSB = (DIVISOR >> 8) & 0xff;
    UART_LCR &= ~0x80;
    UART_LCR |= 0x3;
    UART_FCR = 0x7;
}

inline int __uart_tx_ready() {
    return UART_LSR & (1 << 5);
}

void __am_uart_tx(AM_UART_TX_T *tx) {
    while (!__uart_tx_ready());
    UART_THR = tx->data;
}

inline int __uart_rx_ready() {
    return UART_LSR & (1 << 0);
}

void __am_uart_rx(AM_UART_RX_T *rx) {
    if (__uart_rx_ready()) {
        rx->data = UART_RBR;
    } else {
        rx->data = 0xff;
    }
}
