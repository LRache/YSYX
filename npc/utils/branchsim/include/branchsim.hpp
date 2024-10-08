#ifndef __SIM_BRANCH_HPP__
#define __SIM_BRANCH_HPP__

#include <stdint.h>
#include <iostream>
#include "tracer.hpp"
#include "predictor.hpp"

struct BranchSimResult {
    uint64_t success;
    uint64_t fail;
};

template <typename addr_t>
BranchSimResult branch_sim(Predictor<addr_t> &predictor, TracerReader<MemTracerEntry<addr_t>> &reader) {
    BranchSimResult result = {};
    auto entry = reader.begin();
    addr_t pc = entry.addr;
    while (!reader.is_end()) {
        addr_t pnpc = predictor.predict(pc);
        entry = reader.next();
        if (reader.is_end()) break;
        bool s = pnpc == entry.addr;
        if (s) {
            result.success++;
        } else {
            result.fail++;
        }
        predictor.update(pc, s);
        pc = entry.addr;
    }
    return result;
}

#endif