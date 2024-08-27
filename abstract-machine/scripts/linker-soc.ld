ENTRY(_start)

MEMORY {
  MROM : ORIGIN = 0x20000000, LENGTH = 4K
  SRAM : ORIGIN = 0x0f000000, LENGTH = 8K
  SDRAM : ORIGIN = 0xa0000000, LENGTH = 32M
  FLASH : ORIGIN = 0x30000000, LENGTH = 256M
}

SECTIONS {
  . = ORIGIN(SDRAM);
  .entry : 
  {
    /* . = ORIGIN(FLASH); */
    *(.entry)
  } > FLASH AT > FLASH

  .bootloader :
  {
    /* . = ORIGIN(SDRAM); */
    _bootloader_start = .;
    *(.bootloader)
    . = ALIGN(0x4);
  } > SDRAM AT> FLASH

  .text :
  {
    _text_start = .;
    *(.text*)
    . = ALIGN(0x4);
  } > SDRAM AT> FLASH

  .rodata ALIGN(0x4) :
  {
    _rodata_start = .;
    *(.rodata*)
    *(.srodata*)
    . = ALIGN(0x4);
  } > SDRAM AT> FLASH
  
  .data ALIGN(0x4) :
  {
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
}
