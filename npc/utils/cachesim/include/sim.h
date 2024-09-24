#pragma once

#include "tracer.hpp"
#include "cache.h"

struct SimResult {
    uint64_t readHit   = 0;
    uint64_t readMiss  = 0;
    uint64_t writeHit  = 0;
    uint64_t writeMiss = 0;
};

SimResult sim(Cache &cache, TracerReader<MemTracerEntry<word_t>> &reader) {
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
