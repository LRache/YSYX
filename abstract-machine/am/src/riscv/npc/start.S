.section .entry, "ax"
.globl _start
.type _start, @function

_start:
  mv s0, zero
  la sp, _stack_pointer
  call _entry_bootloader
  call _bootloader
  call _trm_init
