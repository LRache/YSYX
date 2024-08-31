#pragma once

#include "trace.h"
#include "cache.h"

struct SimResult {
    uint64_t readHit;
    uint64_t readMiss;
    uint64_t writeHit;
    uint64_t writeMiss;
};

template <typename T>
SimResult sim(Cache &cache, Tracer<T> &tracer) {
    SimResult result = {};
    for (MemTracerAddr addr : tracer) {
        if (addr.t == MemType::READ) {
            if (cache.read(addr.addr)) result.readHit ++;
            else result.readMiss ++;
        } else if (addr.t == MemType::WRITE) {
            if (cache.write(addr.addr)) result.writeHit ++;
            else result.writeMiss ++;
        }
    }
    return result;
}

SimResult sim(Cache &cache, MemTracerReader &reader) {
    SimResult result = {};
    MemTracerAddr addr;
    for (; !reader.is_end(); addr = reader.next()) {
        if (addr.t == MemType::READ) {
        if (cache.read(addr.addr)) result.readHit ++;
            else result.readMiss ++;
        } else if (addr.t == MemType::WRITE) {
            if (cache.write(addr.addr)) result.writeHit ++;
            else result.writeMiss ++;
        }
        // std::cout << std::hex << addr.addr << std::endl;
    }
    return result;
}
