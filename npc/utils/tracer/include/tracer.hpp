#ifndef __TRACER_H__
#define __TRACER_H__

#include <stdint.h>
#include <string>
#include <ostream>
#include <istream>
#include <cassert>

enum MemType{ READ, WRITE };

template<typename addr_t>
struct MemTracerEntry
{
    addr_t addr;
    MemType memType;
};


template<typename Iterator>
class Tracer {
public:
    virtual Iterator begin() const = 0;
    virtual Iterator end() const = 0;
};

template <typename Entry>
class TracerWriter {
public:
    virtual bool open(const std::string &filename) { return false; }
    virtual void open(std::ostream &stream) {}
    virtual bool close() { return false; }
    virtual void trace(const Entry &data) { assert(0); }
    
    virtual ~TracerWriter() = default;
};

template <typename Entry>
class TracerReader {
public:
    virtual bool open(const std::string &filename) { return false; }
    virtual void open(std::istream &stream) {}
    virtual bool close() { return false; }
    virtual Entry begin() { assert(0); }
    virtual Entry next() { assert(0); }
    virtual bool is_end() const = 0;
};

class MemTracerReader {
public:
    virtual ~MemTracerReader() = default;
    virtual bool is_end() const = 0;
    virtual void close() = 0;
};

#endif
