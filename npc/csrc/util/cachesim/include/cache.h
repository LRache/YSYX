#ifndef __CACHESIM_H__
#define __CAHCESIM_H__

#include "itrace.h"

class Cache {
private:
    int e; // count of entries of each group
    int s; // count of group
    int b; // count of bytes in one entry
    
    uint32_t *tag;
    bool *valid;
    uint32_t tagMask;
public:
    Cache(int _e, int _s, int _b);
    bool mem(word_t addr);
    ~Cache();
};

#endif
