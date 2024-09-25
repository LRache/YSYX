#include "itracer.hpp"
#include "branchsim.hpp"
#include "normalPredictor.hpp"

#include <iostream>
#include <sstream>

#define ADDR_BASE 0x30000000

using word_t = uint32_t;

int main() {
    std::stringstream byteStream(std::ios::in | std::ios::out | std::ios::binary);
    ITracerWriter<word_t> writer;
    writer.open(byteStream);
    for (uint32_t addr = ADDR_BASE; addr < ADDR_BASE + 0x20; addr += 4) {
        writer.trace(addr);
    }
    for (uint32_t addr = ADDR_BASE + 0x40; addr < ADDR_BASE + 0x60; addr += 4) {
        writer.trace(addr);
    }
    writer.end();
    
    byteStream.seekg(0);
    ITracerReader<word_t> reader;
    reader.open(byteStream);
    
    NormalPredictor<word_t> p;
    auto r = branch_sim(p, reader);
    std::cout << r.success << " " << r.fail << std::endl;
    
    return 0;
}
