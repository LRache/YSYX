ARCH ?= riscv32e-ysyxsoc
ALL ?= microbench
TYPES ?= itrace

TRACE_DIR ?= $(NPC_HOME)/output
# TRACE_FILES ?= $(foreach name, $(ALL), $(foreach type, $(TYPES), $(TRACE_DIR)/$(name)-$(ARCH)/$(type).bin))
TRACE_FILES = /home/rache/Documents/code/ysyx-workbench/npc/trace/ictrace.bin

CXX = g++

BUILD_DIR=$(abspath ./build)
EXE=$(BUILD_DIR)/cachesim
SRCS += $(abspath $(shell find ./ -name "*.cpp"))
SRCS += $(abspath $(shell find ../temp-decompress/src -name "*.cpp"))

INC_PATH += $(abspath ./include)
INC_PATH += $(abspath ../tracer/include)
INC_PATH += $(abspath ../temp-decompress/include)
CXX_FLAGS += $(addprefix -I, $(INC_PATH))
CXX_FLAGS += -Og

HEADERS = $(abspath $(shell find $(INC_PATH) -name "*.h" -or -name "*.hpp"))

$(EXE): $(SRCS) $(HEADERS)
	mkdir -p $(BUILD_DIR)
	$(CXX) $(SRCS) $(CXX_FLAGS) -o $(EXE)

run: $(EXE)
	$(EXE) $(TRACE_FILES)

all: run

clean:
	rm $(EXE)
