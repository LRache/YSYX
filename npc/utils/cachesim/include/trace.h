#pragma once

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

class MemTracerReader {
public:
    virtual ~MemTracerReader() = default;
    virtual MemTracerAddr begin() = 0;
    virtual MemTracerAddr next() = 0;
    virtual bool is_end() const = 0;
    virtual void close() = 0;
};
