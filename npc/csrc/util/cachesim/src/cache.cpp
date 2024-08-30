#include "cache.h"
#include <math.h>
#include <iostream>

Cache::Cache(int _e, int _s, int _b) {
    e = 1 << _e;
    s = 1 << _s;
    b = 1 << _b;

    this->cache = new word_t[e*s*b];
    this->tag = new word_t[e*s];
    this->valid = new bool[e*s];

    int tagLength = (sizeof(word_t) << 3) - _s - _b - (int)log2(sizeof(word_t));
    std::cout << tagLength << (int)log2(sizeof(word_t)) << std::endl;
}

Cache::~Cache() {
    delete this->cache;
    delete this->tag;
    delete this->valid;
}
