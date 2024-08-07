typedef unsigned char uint8_t;
typedef unsigned short uint16_t;

#define UART_BASE 0x10000000
#define UART_LCR (*(volatile uint8_t *)(UART_BASE + 3))
#define UART_THR (*(volatile uint8_t *)(UART_BASE + 0))
#define UART_LSB (*(volatile uint8_t *)(UART_BASE + 0))
#define UART_MSB (*(volatile uint8_t *)(UART_BASE + 1))
#define UART_FCR (*(volatile uint8_t *)(UART_BASE + 2))
#define UART_LSR (*(volatile uint8_t *)(UART_BASE + 5))

#define DIVISOR (uint16_t)(12000000 / (16 * 9600))

void _uart_init() {
    UART_LCR |= 0x80;
    UART_LSB = DIVISOR & 0xff;
    UART_MSB = (DIVISOR >> 8) & 0xff;
    UART_LCR &= ~0x80;
    UART_LCR |= 0x3;
    UART_FCR = 0x7;
}

inline int _uart_send_byte_ready() {
    return UART_LSR & 0x20;
}

void _uart_putch(char ch) {
    while (!_uart_send_byte_ready());
    UART_THR = ch;
}
