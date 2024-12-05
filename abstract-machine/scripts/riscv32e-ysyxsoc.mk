include $(AM_HOME)/scripts/isa/riscv.mk
include $(AM_HOME)/scripts/platform/ysyxsoc.mk
COMMON_CFLAGS += -mabi=ilp32e  # overwrite
LDFLAGS       += -melf32lriscv # overwrite

ifeq ($(CExtension), y)
    $(info CExtension is enabled)
    COMMON_CFLAGS += -march=rv32ec_zicsr
    NPCFLAGS += --nodifftest
else
    $(info CExtension is disabled)
    COMMON_CFLAGS += -march=rv32e_zicsr
endif

AM_SRCS += riscv/npc/libgcc/div.S \
           riscv/npc/libgcc/muldi3.S \
           riscv/npc/libgcc/multi3.c \
           riscv/npc/libgcc/ashldi3.c \
           riscv/npc/libgcc/unused.c