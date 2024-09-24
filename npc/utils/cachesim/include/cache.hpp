#ifndef __CACHE_H__
#define __CAHCE_H__

#include "tracer.hpp"
#include <iostream>
#include <vector>

template <typename addr_t>
class Cache {
protected:
    unsigned int E; // count of entries of each group
    unsigned int S; // count of group
    unsigned int B; // count of bytes in one entry
    unsigned int e;
    unsigned int s;
    unsigned int b;
    
    std::vector<std::vector<addr_t>> tag;
    addr_t tagMask;
    addr_t indexMask;
    std::vector<std::vector<bool>> valid;
public:
    Cache(unsigned int _e, unsigned int _s, unsigned int _b);
    virtual ~Cache() = default;
    bool is_valid(unsigned int groupIndex, unsigned int entryIndex);
    bool read(addr_t addr);
    bool write(addr_t addr);
    virtual unsigned int get_replace_entry(unsigned int groupIndex) = 0;
    virtual void hit(unsigned int groupIndex, unsigned int entryIndex, bool isRead) = 0;
};

template <typename T>
static inline T set_high_bits(unsigned int count) {
    const size_t bitsNum = sizeof(T) << 3;
    return (~T(0)) << (bitsNum - count);
}

template <typename addr_t>
Cache<addr_t>::Cache(unsigned int _e, unsigned int _s, unsigned int _b) : e(_e), s(_s), b(_b) {
    E = 1 << e;
    S = 1 << s;
    B = 1 << b;

    this->tag = std::vector<std::vector<addr_t>>(S, std::vector<addr_t>(E, 0));
    this->valid = std::vector<std::vector<bool>>(S, std::vector<bool>(E, false));

    size_t ADDR_LENGTH = sizeof(addr_t) << 3;
    addr_t t = 1 << (ADDR_LENGTH - 1);
    int tagLength = ADDR_LENGTH - s - b;
    this->tagMask = set_high_bits<addr_t>(tagLength - 1);
    this->indexMask = set_high_bits<addr_t>(ADDR_LENGTH - b) & (~this->tagMask);
    // std::cout << tagLength << std::endl;
}

template <typename addr_t>
bool Cache<addr_t>::is_valid(unsigned int groupIndex, unsigned int entryIndex) {
    return valid[groupIndex][entryIndex];
}

template <typename addr_t>
bool Cache<addr_t>::read(addr_t addr) {
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

template <typename addr_t>
bool Cache<addr_t>::write(addr_t addr) {
    return false;
}

#endif
