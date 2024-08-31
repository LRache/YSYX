#include "cache.h"
#include <math.h>
#include <iostream>

Cache::Cache(int _e, int _s, int _b) : e(_e), s(_s), b(_b) {
    E = 1 << e;
    S = 1 << s;
    B = 1 << b;

    this->tag = new word_t[E*S];
    this->valid = new bool[E*S];
    std::fill(this->valid, this->valid + E*S, false);
    this->counter = new unsigned int[S];
    std::fill(this->counter, this->counter + S, 0);

    int tagLength = (sizeof(word_t) << 3) - s - b;
    this->tagMask = ((int)0x80000000) >> tagLength;
    this->indexMask = ((0x80000000) >> (32 - b)) & (~this->tagMask);
    // std::cout << std::hex << this->tagMask << std::endl << std::dec;
}

bool Cache::read(word_t addr) {
    int index = (addr & indexMask) >> b;
    for (int i = 0; i < E; i++) {
        if (this->tag[index * S + i] == (addr & this->tagMask) && this->valid[index * S + i]) {
            return true;
        }
    }
    this->tag[index * S + counter[index]] = addr & this->tagMask;
    this->valid[index * S + counter[index]] = true;
    counter[index] = (counter[index] + 1) % this->E;
    return false;
}

bool Cache::write(word_t addr) {
    return false;
}

SimResult Cache::sim(Tracer &tracer) {
    word_t addr;
    Tracer::Type t;
    SimResult result = {};
    tracer.iter_init();
    while (!tracer.iter_is_end()) {
        addr = tracer.iter_next(&t);
        if (t == Tracer::Type::READ) {
            if (this->read(addr)) {
                result.readHit++;
            } else {
                result.readMiss++;
            }
        } else if (t == Tracer::Type::WRITE) {
            if (this->write(addr)) {
                result.writeHit++;
            } else {
                result.writeMiss++;
            }
        }
    }
    return result;
}

Cache::~Cache() {
    delete this->tag;
    delete this->valid;
}
