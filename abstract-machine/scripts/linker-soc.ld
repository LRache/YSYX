ENTRY(_start)
/* PHDRS { text PT_LOAD; data PT_LOAD; } */

MEMORY {
  MROM : ORIGIN = 0x20000000, LENGTH = 4K
  SRAM : ORIGIN = 0x0f000000, LENGTH = 8K
}

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  . = _pmem_start + _entry_offset;
  .text : {
    *(entry)
    *(.text*)
  } > MROM
  etext = .;
  _etext = .;

  .rodata : {
    *(.rodata*)
    *(.srodata*)
    _rodata_end = .;
  } > MROM
  
  .data : AT (_rodata_end)
  {
    _data_start = .;
    *(.data*)
    *(.sdata*)
  } > SRAM
  
  _data_size = SIZEOF(.data);
  _data_load_start = LOADADDR(.data);

  edata = .;
  _data = .;
  .bss : {
	_bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
  } > SRAM

  _stack_top = ALIGN(0x1000);
  . = _stack_top + 0x8000;
  _stack_pointer = .;
  end = .;
  _end = .;
  _heap_start = ALIGN(0x1000);
}
