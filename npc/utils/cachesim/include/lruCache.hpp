#ifndef __LRU_CACHE_H__
#define __LRU_CACHE_H__

#include "cache.hpp"

template <typename addr_t>
class LRUCache : public Cache<addr_t> {
private:
    std::vector<std::vector<uint32_t>> counter;
public:
    LRUCache(unsigned int, unsigned int, unsigned int);
    unsigned int get_replace_entry(unsigned int groupIndex) override;
    void hit(unsigned int groupIndex, unsigned int entryIndex, bool isRead) override;
};

template <typename addr_t>
LRUCache<addr_t>::LRUCache(unsigned int _e, unsigned int _s, unsigned int _b) : Cache<addr_t>(_s, _s, _b) {
    counter = std::vector<std::vector<uint32_t>>(this->S, std::vector<uint32_t>(this->E, 0));
}

template <typename addr_t>
unsigned int LRUCache<addr_t>::get_replace_entry(unsigned int groupIndex) {
    uint32_t choose = 0;
    for (int i = 0; i < this->E; i++) {
        if (this->is_valid(groupIndex, i)) {
            choose = counter[groupIndex][i] < counter[groupIndex][choose] ? i : choose;
        } else {
            return i;
        }
    }
    return choose;
}

template <typename addr_t>
void LRUCache<addr_t>::hit(unsigned int groupIndex, unsigned int entryIndex, bool isRead) {
    this->counter[groupIndex][entryIndex] ++;
}

#endif