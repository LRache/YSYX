#ifndef __TRACER_H__
#define __TRACER_H__

#include <stdint.h>

typedef uint32_t word_t;

enum MemType{ READ, WRITE };

struct MemTracerAddr
{
    word_t addr;
    MemType t;
};

template<typename Iterator>
class Tracer {
public:
    virtual Iterator begin() const = 0;
    virtual Iterator end() const = 0;
};

#endif
