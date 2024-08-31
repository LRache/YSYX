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

    int tagLength = (sizeof(word_t) << 3) - s - b;
    this->tagMask = ((int)0x80000000) >> tagLength;
    this->indexMask = ((0x80000000) >> (32 - b)) & (~this->tagMask);
}

bool Cache::read(word_t addr) {
    uint32_t groupIndex = (addr & indexMask) >> b;
    for (int i = 0; i < E; i++) {
        if (this->tag[groupIndex * S + i] == (addr & this->tagMask) && this->valid[groupIndex * S + i]) {
            hit(groupIndex, i, true);
            return true;
        }
    }
    uint32_t replace = get_replace_entry(groupIndex);
    this->tag[groupIndex * S + replace] = addr & this->tagMask;
    this->valid[groupIndex * S + replace] = true;
    return false;
}

bool Cache::write(word_t addr) {
    return false;
}

Cache::~Cache() {
    delete this->tag;
    delete this->valid;
}

FIFOCache::FIFOCache(int _e, int _s, int _b) : Cache(_e, _s, _b) {
    this->counter = new unsigned int[S];
    std::fill(this->counter, this->counter + S, 0);
}

uint32_t FIFOCache::get_replace_entry(uint32_t groupIndex) {
    uint32_t t = counter[groupIndex];
    counter[groupIndex] = (counter[groupIndex] + 1) % E;
    return t;
}
