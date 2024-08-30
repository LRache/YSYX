#include "cache.h"
#include <math.h>
#include <iostream>

Cache::Cache(int _e, int _s, int _b) {
    e = 1 << _e;
    s = 1 << _s;
    b = 1 << _b;

    this->tag = new word_t[e*s];
    this->valid = new bool[e*s];
    std::fill(this->valid, this->valid + e*s, false);

    int tagLength = (sizeof(word_t) << 3) - _s - _b - (int)log2(sizeof(word_t));
    this->tagMask = (0x80000000) >> tagLength;
    std::cout << tagLength << (int)log2(sizeof(word_t)) << std::endl;
}

bool Cache::mem(word_t addr) {

}

Cache::~Cache() {
    delete this->tag;
    delete this->valid;
}
