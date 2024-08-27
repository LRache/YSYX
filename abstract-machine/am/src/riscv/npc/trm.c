#include <am.h>
#include <klib-macros.h>
#include <klib.h>
#include "../riscv.h"

extern char _heap_start;
int main(const char *args);

#define HEAP_SIZE (8 * 1024 * 1024)
#define HEAP_END  ((uintptr_t)&_heap_start + HEAP_SIZE)

Area heap = RANGE(&_heap_start, HEAP_END);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

void putch(char ch) {
  io_write(AM_UART_TX, ch);
}

void _trm_init() {
  ioe_init();
  
  int ret = main(mainargs);
  halt(ret);
}


void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));

  while (1);
}
