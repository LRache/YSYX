__PWD=$(shell pwd)
CSRC_DIR=./csrc
VSRC_DIR=./vsrc
SIM_DIR=./sim
M=
CPP_DIR=$(CSRC_DIR)/sim_$(M)
VSRC=$(VSRC_DIR)/$(M).v
CSRC=$(CSRC_DIR)/sim_$(M).cpp
EXE=./temp/V$(M)
SIM_EXE=$(SIM_DIR)/sim_$(M)

cpp: $(CPP_DIR)/V$(M).mk

$(CPP_DIR)/V$(M).mk: $(VSRC)
	mkdir -p $(CPP_DIR)
	verilator --cc $(VSRC) --Mdir $(CPP_DIR) --trace-fst

all: sim

.PHONY sim: $(SIM_EXE)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!
	cd $(SIM_DIR) && ./sim_$(M)

$(SIM_EXE): $(CSRC) $(VSRC)
	verilator --cc --exe -Wall $(CSRC) $(VSRC) -Mdir ./temp --trace-fst
	make -C temp -f V$(M).mk V$(M)
	mkdir -p ./sim
	mv $(EXE) $(SIM_EXE)
	rm -rf ./temp

include ../Makefile
