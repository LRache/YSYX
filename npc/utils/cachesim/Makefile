ARCH ?= riscv32e-ysyxsoc
ALL ?= microbench
TYPES ?= itrace

TRACE_DIR ?= $(NPC_HOME)/output
TRACE_FILES ?= $(foreach name, $(ALL), $(foreach type, $(TYPES), $(TRACE_DIR)/$(name)-$(ARCH)/$(type).bin))

CXX = g++

BUILD_DIR=$(abspath ./build)
EXE=$(BUILD_DIR)/cachesim
SRCS = $(abspath $(shell find ./ -name "*.cpp"))
HEADERS = $(abspath $(shell find ./include -name "*.h"))

INC_PATH += $(abspath ./include)
CXX_FLAGS += $(addprefix -I, $(INC_PATH))

$(EXE): $(SRCS) $(HEADERS)
	mkdir -p $(BUILD_DIR)
	$(CXX) $(SRCS) $(CXX_FLAGS) -o $(EXE)

run: $(EXE)
	$(EXE) $(TRACE_FILES)

all: run
