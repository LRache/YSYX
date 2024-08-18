AM_SRCS := riscv/npc/start.S \
           riscv/npc/trm.c \
           riscv/npc/ioe/ioe.c \
           riscv/npc/ioe/timer.c \
           riscv/npc/ioe/input.c \
           riscv/npc/ioe/uart.c \
           riscv/npc/cte.c \
           riscv/npc/trap.S \
           riscv/npc/bootloader.c \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
LDFLAGS   += -T $(AM_HOME)/scripts/linker-soc.ld \
						 --defsym=_pmem_start=0xa0000000 --defsym=_entry_offset=0x0
LDFLAGS   += --gc-sections -e _start
CFLAGS += -DMAINARGS=\"$(mainargs)\"
.PHONY: $(AM_HOME)/am/src/riscv/npc/trm.c

image: $(IMAGE).elf
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S -O binary $(IMAGE).elf $(IMAGE).bin
    # @$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin

NPC_EXE=$(NPC_HOME)/sim/VysyxSoCFull

.PHONY run: image
	$(NPC_EXE) -f $(IMAGE).bin
