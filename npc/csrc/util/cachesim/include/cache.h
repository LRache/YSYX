#ifndef __CACHESIM_H__
#define __CAHCESIM_H__

#include "trace.h"
#include <iostream>

struct SimResult {
    uint64_t readHit;
    uint64_t readMiss;
    uint64_t writeHit;
    uint64_t writeMiss;
};

class Cache {
private:
    int E; // count of entries of each group
    int S; // count of group
    int B; // count of bytes in one entry
    int e;
    int s;
    int b;
    
    uint32_t *tag;
    uint32_t tagMask;
    uint32_t indexMask;
    uint32_t *counter;
    bool *valid;
public:
    Cache(int _e, int _s, int _b);
    bool read(word_t addr);
    bool write(word_t addr);
    template <typename T> SimResult sim(Tracer<T> &tracer);
    ~Cache();
};

template <typename T>
SimResult Cache::sim(Tracer<T> &tracer) {
    word_t addr;
    MemType t;
    SimResult result = {};
    for (auto pc : tracer) {
        std::cout << pc.addr << std::endl;
    }
    return result;
}

#endif
