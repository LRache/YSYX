#pragma once

#include "trace.h"
#include "cache.h"

struct SimResult {
    uint64_t readHit   = 0;
    uint64_t readMiss  = 0;
    uint64_t writeHit  = 0;
    uint64_t writeMiss = 0;
};

template <typename T>
SimResult sim(Cache &cache, Tracer<T> &tracer) {
    SimResult result = {};
    // std::cout << std::hex;
    for (MemTracerAddr addr : tracer) {
        if (addr.t == MemType::READ) {
            if (cache.read(addr.addr)) result.readHit ++;
            else result.readMiss ++;
        } else if (addr.t == MemType::WRITE) {
            if (cache.write(addr.addr)) result.writeHit ++;
            else result.writeMiss ++;
        }
        // std::cout << addr.addr << std::endl;
    }
    return result;
}

SimResult sim(Cache &cache, MemTracerReader &reader) {
    SimResult result = {};
    int c = 0;
    for (MemTracerAddr addr = reader.begin(); !reader.is_end(); addr = reader.next()) {
        if (addr.t == MemType::READ) {
            if (cache.read(addr.addr)) result.readHit ++;
            else result.readMiss ++;
        } else if (addr.t == MemType::WRITE) {
            if (cache.write(addr.addr)) result.writeHit ++;
            else result.writeMiss ++;
        } else {
            std::cout << std::hex;
            std::cout << addr.t << " " << addr.addr << " " << c << std::endl;
            std::cout << std::dec;
        }
        c++;
        // std::cout << std::hex << addr.addr << std::endl;
    }
    // std::cout << c << std::endl;
    return result;
}
