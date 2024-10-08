CXX = g++

INC_PATH += $(abspath ./include)
INC_PATH += $(abspath ../temp-decompress/include)
INC_PATH += $(abspath ../tracer/include)
INCFLAGS += $(addprefix -I, $(INC_PATH))
CSRCS += $(shell find ./src  -name "*.cpp")
CSRCS += $(shell find ../temp-decompress/src -name "*.cpp")
HEADERS += $(shell find ./include -name "*.hpp" -or -name "*.h")
CXXFLAGS += $(INCFLAGS)
CXXFLAGS += -Wall -Werror
BUILD_DIR = $(abspath ./build)

TARGET = $(BUILD_DIR)/branchsim

$(TARGET): $(CSRCS) $(HEADERS)
	mkdir -p ./build
	$(CXX) $(CXXFLAGS) $(CSRCS) -o $(TARGET)

run: $(TARGET)
	$(TARGET)

all: $(TARGET)

clean: 
	rm $(TARGET)