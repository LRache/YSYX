#ifndef __SIM_HPP__
#define __SIM_HPP__

#include <iostream>

#include "tracer.hpp"
#include "cache.hpp"

struct SimResult {
    uint64_t readHit   = 0;
    uint64_t readMiss  = 0;
    uint64_t writeHit  = 0;
    uint64_t writeMiss = 0;
};

template <typename addr_t>
SimResult sim(Cache<addr_t> &cache, TracerReader<MemTracerEntry<addr_t>> &reader) {
    SimResult result = {};
    for (auto entry = reader.begin(); !reader.is_end(); entry = reader.next()) {
        if (entry.memType == MemType::READ) {
            if (cache.read(entry.addr)) result.readHit ++;
            else result.readMiss ++;
        } else if (entry.memType == MemType::WRITE) {
            if (cache.write(entry.addr)) result.writeHit ++;
            else result.writeMiss ++;
        } else {
            std::cerr << "Unkown mem type" << std::endl;
        }
    }
    return result;
}

#endif
