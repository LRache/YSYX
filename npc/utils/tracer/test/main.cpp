#include "itracer.hpp"

#include <iostream>
#include <sstream>

#define ADDR_BASE 0x30000000

int main() {
    std::stringstream byteStream(std::ios::in | std::ios::out | std::ios::binary);
    ITracerWriter writer;
    writer.open(byteStream);
    for (uint32_t addr = ADDR_BASE; addr < ADDR_BASE + 0x20; addr += 4) {
        writer.trace(addr);
    }
    for (uint32_t addr = ADDR_BASE + 0x40; addr < ADDR_BASE + 0x60; addr += 4) {
        writer.trace(addr);
    }
    writer.end();
    
    byteStream.seekg(0);
    
    ITracerReader reader;
    reader.open(byteStream);
    std::cout << std::endl;
    for (uint32_t addr = reader.begin(); !reader.is_end(); addr = reader.next()) {
        std::cout << std::hex << addr << std::endl;
    }
    
    return 0;
}
