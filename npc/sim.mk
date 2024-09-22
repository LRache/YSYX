.PHONY sim: $(SIM_EXE)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	cd $(SIM_DIR) && $(SIM_EXE) $(SIM_FLAGS)