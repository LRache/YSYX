VERILATOR= verilator
VERILATOR_CFLAGS += -MMD --build -cc -O3 --trace-fst 
VERILATOR_FLAGS += --timescale "1ns/1ns" --no-timing
INC_PATH += $(abspath ./csrc/include)
INC_PATH += $(abspath ./obj)

m = Top
ifeq ($(m),soc)
TOP=ysyxSoCFull
mainModule=cpu.CPU
else
TOP=$(lastword $(subst ., ,$(m)))
mainModule=$(m)
endif

ifneq ($(mem),)
SIM_FLAGS += -m $(abspath $(mem))
endif
ifneq ($(rom),)
SIM_FLAGS += -r $(abspath $(rom))
endif
ifneq ($(flash),)
SIM_FLAGS += -f $(abspath $(flash))
endif

SSRC_DIR = $(abspath ./ssrc)
CSRC_DIR = $(abspath ./csrc)
VSRC_DIR = $(abspath ./vsrc)
OBJ_DIR	 = $(abspath ./obj/V$(TOP))
SIM_DIR  = $(abspath ./sim)
SIM_EXE  = $(abspath $(SIM_DIR)/V$(TOP))
TEMP_DIR = $(abspath ./temp)
TEMP_EXE = $(TEMP_DIR)/V$(TOP)

SSRCS = $(abspath $(shell find $(SSRC_DIR) -name "*.scala"))
VSRCS = $(abspath $(shell find $(VSRC_DIR) -name "*.v" -or -name "*.sv"))
CSRCS = $(abspath $(shell find $(CSRC_DIR) -name "*.c" -or -name "*.cc" -or -name "*.cpp"))

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
VINCFLAGS = $(addprefix -I, $(VINC_PATH))
VERILATOR_FLAGS += $(VINCFLAGS)

CXXFLAGS += $(addprefix -DVTop=, V$(TOP))
CXXFLAGS += $(addprefix -DVTopHeader=, \"V$(TOP)/V$(TOP).h\")

LDFLAGS += -dynamic-linker $(NEMU_SO)

PRJ = playground

test:
	mill -i $(PRJ).test

verilog: $(SSRCS)
	$(call git_commit, "generate verilog")
	mkdir -p $(VSRC_DIR)
	mill -i $(PRJ).runMain $(mainModule) --target-dir $(VSRC_DIR) --target verilog

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

update: $(OBJ_DIR)/V$(TOP).mk

$(OBJ_DIR)/V$(TOP).mk: verilog
	mkdir -p $(OBJ_DIR)
	$(VERILATOR) --cc $(VSRCS) --Mdir $(OBJ_DIR) --trace-fst --top-module $(TOP) $(VERILATOR_FLAGS)

$(SIM_EXE): $(CSRCS) $(VSRCS) $(NVBOARD_ARCHIVE)
	mkdir -p $(SIM_DIR)
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
