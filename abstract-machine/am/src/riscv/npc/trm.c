#include <am.h>
#include <klib-macros.h>
#include <klib.h>
#include "../riscv.h"

extern char _heap_start;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, PMEM_END);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

void _uart_init();
void _uart_putch(char ch);

void putch(char ch) {
  _uart_putch(ch);
}

extern char _data_start[];
extern char _data_size[];
extern char _data_load_start[];

void _trm_init() {
  _uart_init();
  if (_data_start != _data_load_start) {
    memcpy(_data_start, _data_load_start, (size_t)_data_size);
  }
  int ret = main(mainargs);
  halt(ret);
}


void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));

  while (1);
}
