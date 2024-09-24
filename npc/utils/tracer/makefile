CXX = g++

INC_PATH += $(abspath ./include)
INCFLAGS += $(addprefix -I, $(INC_PATH))
CSRCS += $(abspath $(shell find ./src  -name "*.cpp"))
CSRCS += $(abspath $(shell find ./test -name "*.cpp"))
CXXFLAGS += $(INCFLAGS)
CXXFLAGS += -Wall -Werror
TARGET = ./build/main

$(TARGET): $(CSRCS)
	mkdir -p ./build
	$(CXX) $(CSRCS) $(CXXFLAGS) -o $(TARGET)

run: $(TARGET)
	$(TARGET)

all: $(TARGET)

clean: 
	rm $(TARGET)
