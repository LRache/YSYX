#include "cache.h"
#include <math.h>
#include <iostream>

Cache::Cache(int _e, int _s, int _b) : e(_e), s(_s), b(_b) {
    E = 1 << e;
    S = 1 << s;
    B = 1 << b;

    this->tag = std::vector<std::vector<uint32_t>>(S, std::vector<uint32_t>(E, 0));
    this->valid = std::vector<std::vector<bool>>(S, std::vector<bool>(E, false));

    int tagLength = (sizeof(word_t) << 3) - s - b;
    this->tagMask = ((int)0x80000000) >> tagLength;
    this->indexMask = ((int)(0x80000000) >> (32 - b)) & (~this->tagMask);
}

bool Cache::is_valid(uint32_t groupIndex, uint32_t entryIndex) {
    return valid[groupIndex][entryIndex];
}

bool Cache::read(word_t addr) {
    uint32_t groupIndex = (addr & indexMask) >> b;
    for (int i = 0; i < E; i++) {
        if (this->tag[groupIndex][i] == (addr & this->tagMask) && this->valid[groupIndex][i]) {
            hit(groupIndex, i, true);
            return true;
        }
    }
    uint32_t replace = get_replace_entry(groupIndex);
    this->tag[groupIndex][replace] = addr & this->tagMask;
    this->valid[groupIndex][replace] = true;
    return false;
}

bool Cache::write(word_t addr) {
    return false;
}

FIFOCache::FIFOCache(int _e, int _s, int _b) : Cache(_e, _s, _b) {
    counter = std::vector<uint32_t>(S, 0);
}

uint32_t FIFOCache::get_replace_entry(uint32_t groupIndex) {
    uint32_t t = counter[groupIndex];
    counter[groupIndex] = (counter[groupIndex] + 1) % E;
    return t;
}

LRUCache::LRUCache(int _e, int _s, int _b) : Cache(_s, _s, _b) {
    counter = std::vector<std::vector<uint32_t>>(S, std::vector<uint32_t>(E, 0));
}

uint32_t LRUCache::get_replace_entry(uint32_t groupIndex) {
    uint32_t choose = 0;
    for (int i = 0; i < E; i++) {
        if (is_valid(groupIndex, i)) {
            choose = counter[groupIndex][i] < counter[groupIndex][choose] ? i : choose;
        } else {
            return i;
        }
    }
    return choose;
}

void LRUCache::hit(uint32_t groupIndex, uint32_t entryIndex, bool isRead) {
    this->counter[groupIndex][entryIndex] ++;
}
