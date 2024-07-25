VERILATOR= verilator
VERILATOR_CFLAGS += -MMD --build -cc -O3 --trace-fst
INC_PATH ?=

TOP=top
PNAME?=$(TOP)
CSRC_DIR = $(abspath ./csrc/$(PNAME))
VSRC_DIR = $(abspath ./vsrc/$(PNAME))
OBJ_DIR	 = $(CSRC_DIR)/obj
SIM_DIR  = $(abspath ./sim/$(PNAME))
SIM_EXE  = $(SIM_DIR)/$(PNAME)
TEMP_DIR = $(abspath ./temp)
TEMP_EXE = $(TEMP_DIR)/V$(PNAME)

VSRCS = $(shell find $(abspath $(VSRC_DIR)) -name "*.v")
CSRCS = $(shell find $(abspath $(CSRC_DIR)) -name "*.c" -or -name "*.cc" -or -name "*.cpp" | grep -v "^${OBJ_DIR}/")

include $(NVBOARD_HOME)/scripts/nvboard.mk

INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS)

cpp: $(OBJ_DIR)/V$(TOP).mk

$(OBJ_DIR)/V$(TOP).mk: $(VSRCS)
	mkdir -p $(OBJ_DIR)
	$(VERILATOR) --cc $(VSRCS) --Mdir $(OBJ_DIR) --trace-fst --top-module $(TOP)

all: sim

run: sim

.PHONY sim: $(SIM_EXE)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!
	cd $(SIM_DIR) && $(abspath $(SIM_EXE))

$(SIM_EXE): $(CSRCS) $(VSRCS) $(NVBOARD_ARCHIVE)
	@rm -rf $(OBJ_DIR)
	mkdir -p $(SIM_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) --top-module $(TOP) $^ $(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) --Mdir $(OBJ_DIR) --exe -o $(abspath $(SIM_EXE))
	# make -C $(OBJ_DIR) -f V$(TOP).mk V$(TOP)

include ../Makefile
