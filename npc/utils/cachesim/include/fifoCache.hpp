#ifndef __FIFO_CACHE_H__
#define __FIFO_CACHE_H__

#include "cache.hpp"

template <typename addr_t>
class FIFOCache : public Cache<addr_t> {
private:
    std::vector<uint32_t> counter;
public:
    FIFOCache(unsigned int, unsigned int, unsigned int);
    unsigned int get_replace_entry(unsigned int groupIndex) override;
    void hit(unsigned int, unsigned int, bool) override {};
};

template <typename addr_t>
FIFOCache<addr_t>::FIFOCache(unsigned int _e, unsigned int _s, unsigned int _b) : Cache(_e, _s, _b) {
    counter = std::vector<uint32_t>(S, 0);
}

template <typename addr_t>
unsigned int FIFOCache<addr_t>::get_replace_entry(unsigned int groupIndex) {
    uint32_t t = counter[groupIndex];
    counter[groupIndex] = (counter[groupIndex] + 1) % E;
    return t;
}

#endif
