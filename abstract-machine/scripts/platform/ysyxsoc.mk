AM_SRCS := riscv/npc/start.S \
           riscv/npc/trm.c \
           riscv/npc/ioe/ioe.c \
           riscv/npc/ioe/timer.c \
           riscv/npc/ioe/input.c \
           riscv/npc/ioe/uart.c \
           riscv/npc/ioe/gpu.c \
           riscv/npc/cte.c \
           riscv/npc/trap.S \
           riscv/npc/bootloader.c \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
LDFLAGS   += -T $(AM_HOME)/scripts/linker-soc.ld --defsym=_stack_pointer=0xa2000000
LDFLAGS   += --gc-sections -e _start
CFLAGS += -DMAINARGS=\"$(mainargs)\"
.PHONY: $(AM_HOME)/am/src/riscv/npc/trm.c

image: $(IMAGE).elf
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S -O binary $(IMAGE).elf $(IMAGE).bin
    # @$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin

NPC_EXE=$(NPC_HOME)/build/VysyxSoCFull
OUTPUT_DIR=$(NPC_HOME)/output/$(NAME)-$(ARCH)
NPCFLAGS += --flash $(IMAGE).bin

.PHONY run: image
	mkdir -p $(OUTPUT_DIR)
	$(NPC_EXE) $(NPCFLAGS)
