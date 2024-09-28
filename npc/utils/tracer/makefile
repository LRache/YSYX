CXX = g++

INC_PATH += $(abspath ./include)
INCFLAGS += $(addprefix -I, $(INC_PATH))
CSRCS += $(abspath $(shell find ./src -name "*.cpp"))
MAINSRCS += $(abspath ./main.cpp)
TESTSRCS += $(abspath $(shell find ./test -name "*.cpp"))
CXXFLAGS += $(INCFLAGS)
CXXFLAGS += -Wall -Werror
BUILDDIR = ./build
TARGET = $(BUILDDIR)/tracer
TEST_TARGET = $(BUILDDIR)/test

$(TARGET): $(CSRCS) $(MAINSRCS)
	mkdir -p ./build
	$(CXX) $(CSRCS) $(MAINSRCS) $(CXXFLAGS) -o $(TARGET)

run: $(TARGET)
	$(TARGET)

$(TEST_TARGET): $(CSRCS) $(TESTSRCS)
	mkdir -p ./build
	$(CXX) $(CSRCS) $(TESTSRCS) $(CXXFLAGS) -o $(TEST_TARGET)

test: $(TEST_TARGET)
	$(TEST_TARGET)

all: $(TARGET)

clean: 
	rm $(TARGET)
