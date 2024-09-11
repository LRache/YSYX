VERILATOR= verilator
VERILATOR_CFLAGS += -MMD --build -cc -O3 --trace-fst 
VERILATOR_FLAGS += --timescale "1ns/1ns" --no-timing --autoflush

m = Top
ifeq ($(m),soc)
TOP=ysyxSoCFull
mainModule=cpu.CPU
else
TOP=$(lastword $(subst ., ,$(m)))
mainModule=$(m)
endif

UTILS = cachesim

SSRC_DIR = $(abspath ./ssrc)
CSRC_DIR = $(abspath ./csrc)
VSRC_DIR = $(abspath ./vsrc)
BUILD_DIR = $(abspath ./build)
OBJ_DIR	 = $(abspath ./obj/V$(TOP))
SIM_EXE  = $(abspath $(BUILD_DIR)/V$(TOP))
OUTPUT_DIR = $(abspath ./output)
UTILS_DIR = $(abspath ./utils)

SSRCS = $(abspath $(shell find $(SSRC_DIR) -name "*.scala"))
VSRCS = $(abspath $(shell find $(VSRC_DIR) -name "*.v" -or -name "*.sv")) $(abspath $(shell find $(BUILD_DIR) -name "*.v" -or -name "*.sv"))
CSRCS = $(abspath $(shell find $(CSRC_DIR) -name "*.c" -or -name "*.cc" -or -name "*.cpp"))
CSRCS += $(abspath $(shell find $(CSRC_DIR) -name "*.c" -or -name "*.cc" -or -name "*.cpp"))
CSRCS += $(abspath $(shell find $(foreach util,$(UTILS),$(UTILS_DIR)/$(util)/src) -name "*.c" -or -name "*.cc" -or -name "*.cpp"))
INC_PATH += $(abspath ./csrc/include)
INC_PATH += $(abspath ./obj)
INC_PATH += $(abspath $(foreach util,$(UTILS),$(UTILS_DIR)/$(util)/include))

ifeq ($(m),soc)
VSRCS += $(abspath ../ysyxSoC/build/ysyxSoCFull.v)
VSRCS += $(abspath $(shell find ../ysyxSoC/perip -name "*.v"))
VINC_PATH += $(abspath ../ysyxSoC/perip/uart16550/rtl)
VINC_PATH += $(abspath ../ysyxSoC/perip/spi/rtl)
endif

NEMU_SO = $(abspath $(NEMU_HOME)/build/riscv32-nemu-interpreter-so)

include $(NVBOARD_HOME)/scripts/nvboard.mk

INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS) 
CXXFLAGS += -fexceptions
VINCFLAGS = $(addprefix -I, $(VINC_PATH))
VERILATOR_FLAGS += $(VINCFLAGS)

CXXFLAGS += $(addprefix -DVTop=, V$(TOP))
CXXFLAGS += $(addprefix -DVTopHeader=, \"V$(TOP)/V$(TOP).h\")
CXXFLAGS += -I/usr/lib/llvm-14/include -std=c++14   -fno-exceptions -lLLVM-14 -fPIE

LDFLAGS += -dynamic-linker $(NEMU_SO)


ifneq ($(mem),)
SIM_FLAGS += -m $(abspath $(mem))
endif
ifneq ($(rom),)
SIM_FLAGS += -r $(abspath $(rom))
endif
ifneq ($(flash),)
SIM_FLAGS += -f $(abspath $(flash))
endif
SIM_FLAGS += -o $(OUTPUT_DIR)

PRJ = playground

test:
	mill -i $(PRJ).test

verilog: $(SSRCS)
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i $(PRJ).runMain $(mainModule) --target-dir $(BUILD_DIR) --target verilog

help:
	mill -i $(PRJ).runMain mainModule --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

bsp:
	mill -i mill.bsp.BSP/install

idea:
	mill -i mill.idea.GenIdea/idea

clean:
	-rm -rf $(BUILD_DIR)
	-rm -rf $(abspath ./obj)

update: $(OBJ_DIR)/V$(TOP).mk

$(OBJ_DIR)/V$(TOP).mk: verilog
	mkdir -p $(OBJ_DIR)
	$(VERILATOR) --cc $(VSRCS) --Mdir $(OBJ_DIR) --trace-fst --top-module $(TOP) $(VERILATOR_FLAGS)

$(SIM_EXE): $(CSRCS) $(VSRCS) $(NVBOARD_ARCHIVE)
	$(VERILATOR) $(VERILATOR_CFLAGS) $(VERILATOR_FLAGS) --top-module $(TOP) $(CSRCS) $(VSRCS) $(NVBOARD_ARCHIVE) $(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) --Mdir $(OBJ_DIR) --exe -o $(SIM_EXE)

.PHONY sim: $(SIM_EXE)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	cd $(SIM_DIR) && $(SIM_EXE) $(SIM_FLAGS)

run: sim

all: npc

npc: $(SIM_EXE)
	$(call git_commit, "compile RTL")

.PHONY sta:
	cd $(abspath ../yosys-sta/) && make -f $(abspath ../yosys-sta/Makefile) sta SDC_FILE=$(abspath ./vsrc/hcpu.sdc) RTL_FILES="$(VSRCS)" CLK_FREQ_MHZ=100 DESIGN=ysyxSoCFull

-include ../Makefile
