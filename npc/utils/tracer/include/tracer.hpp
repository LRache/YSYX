#ifndef __TRACER_H__
#define __TRACER_H__

#include <stdint.h>
#include <string>
#include <ostream>
#include <istream>
#include <cassert>

enum MemType{ READ, WRITE };

template<typename Iterator>
class Tracer {
public:
    virtual Iterator begin() const = 0;
    virtual Iterator end() const = 0;
};

template <typename Word, typename T>
class TracerWriter {
public:
    virtual bool open(const std::string &filename) { return false; }
    virtual bool open(const std::ostream &stream) { return false; }
    virtual bool close() { return false; }
    virtual void trace(Word data, T type) { assert(0); }
    virtual void trace(Word data) { assert(0); }
    
    virtual ~TracerWriter() = default;
};

template <typename Word, typename T>
class TracerReader {
public:
    virtual bool open(const std::string &filename) { return false; }
    virtual bool open(const std::istream &stream) { return false; }
    virtual bool close() { return false; }
    virtual Word begin() { assert(0); }
    virtual Word begin(T *) { assert(0); }
    virtual Word next() { assert(0); }
    virtual Word next(T *) { assert(0); }
    virtual bool is_end() const = 0;
};

class MemTracerReader {
public:
    virtual ~MemTracerReader() = default;
    // virtual MemTracerAddr begin() = 0;
    // virtual MemTracerAddr next() = 0;
    virtual bool is_end() const = 0;
    virtual void close() = 0;
};

#endif
