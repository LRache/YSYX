ENTRY(_start)
/* PHDRS { text PT_LOAD; data PT_LOAD; } */

MEMORY {
  MROM : ORIGIN = 0x20000000, LENGTH = 4K
  SRAM : ORIGIN = 0x0f000000, LENGTH = 8K
  SDRAM : ORIGIN = 0xa0000000, LENGTH = 32M
  FLASH : ORIGIN = 0x30000000, LENGTH = 256M
}

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  . = _pmem_start + _entry_offset;
  .entry : 
  {
    *(.entry)
  } AT> FLASH

  .bootloader :
  {
    . = ALIGN(0x4);
    _bootloader_start = .;
    *(.bootloader)
    . = ALIGN(0x4);
  } > SDRAM AT> FLASH

  .text :
  {
    . = ALIGN(0x4);
    _text_start = .;
    *(.text*)
    . = ALIGN(0x4);
  } > SDRAM AT> FLASH
  /* etext = .;
  _etext = .; */

  .rodata ALIGN(0x4) :
  {
    . = ALIGN(0x4);
    _rodata_start = .;
    *(.rodata*)
    *(.srodata*)
    . = ALIGN(0x4);
  } > SDRAM AT> FLASH
  
  .data ALIGN(0x4) :
  {
    . = ALIGN(0x4);
    _data_start = .;
    *(.data*)
    *(.sdata*)
    . = ALIGN(0x4);
  } > SDRAM AT> FLASH
  
  _bootloader_size = SIZEOF(.bootloader);
  _bootloader_load_start = LOADADDR(.bootloader);
  _text_size = SIZEOF(.text);
  _text_load_start = LOADADDR(.text);
  _rodata_size = SIZEOF(.rodata);
  _rodata_load_start = LOADADDR(.rodata);
  _data_size = SIZEOF(.data);
  _data_load_start = LOADADDR(.data);

  .bss ALIGN(0x4): 
  {
    _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    . = ALIGN(0x4);
  } > SDRAM AT> FLASH

  _heap_start = ALIGN(0x1000);
  /* _stack_top = ALIGN(0x1000);
  . = _stack_top + 0x8000;
  end = .;
  _end = .;
   */
   /* _stack_pointer = 0x0f002000; */
   _stack_pointer = 0xa2000000;
}
