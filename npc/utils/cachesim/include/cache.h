#pragma once

#include "tracer.h"
#include <iostream>
#include <vector>

class Cache {
protected:
    int E; // count of entries of each group
    int S; // count of group
    int B; // count of bytes in one entry
    int e;
    int s;
    int b;
    
    std::vector<std::vector<uint32_t>> tag;
    uint32_t tagMask;
    uint32_t indexMask;
    std::vector<std::vector<bool>> valid;
public:
    Cache(int _e, int _s, int _b);
    virtual ~Cache() = default;
    bool is_valid(uint32_t groupIndex, uint32_t entryIndex);
    bool read(word_t addr);
    bool write(word_t addr);
    virtual uint32_t get_replace_entry(uint32_t groupIndex) = 0;
    virtual void hit(uint32_t groupIndex, uint32_t entryIndex, bool isRead) = 0;
};

class FIFOCache : public Cache {
private:
    std::vector<uint32_t> counter;
public:
    FIFOCache(int, int, int);
    uint32_t get_replace_entry(uint32_t groupIndex) override;
    void hit(uint32_t, uint32_t, bool) override {};
};

class LRUCache : public Cache {
private:
    std::vector<std::vector<uint32_t>> counter;
public:
    LRUCache(int, int, int);
    uint32_t get_replace_entry(uint32_t groupIndex) override;
    void hit(uint32_t groupIndex, uint32_t entryIndex, bool isRead) override;
};
