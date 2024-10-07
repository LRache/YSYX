#ifndef __DTRACER_HPP__
#define __DTRACER_HPP__

#include "tracer.hpp"
#include <cassert>
#include <istream>
#include <ostream>
#include <fstream>
#include <string>

template <typename addr_t>
class DTracerWriter : public TracerWriter<MemTracerEntry<addr_t>> {
private:
    std::ofstream fstream;
    std::ostream *stream;
public:
    bool open(const std::string &filename) override;
    void open(std::ostream &ostream) override;
    bool close() override;
    void trace(const MemTracerEntry<addr_t> &data) override;
};

template <typename addr_t>
class DTracerReader : public TracerReader<MemTracerEntry<addr_t>> {
private:
    std::ifstream fstream;
    std::istream *stream;
    MemTracerEntry<addr_t> nextEntry;
    bool isEnd = false;
    void read_next();
public:
    bool open(const std::string &filename) override;
    void open(std::istream &istream) override;
    bool close() override;
    MemTracerEntry<addr_t> begin() override;
    MemTracerEntry<addr_t> next() override;
    bool is_end() const override;
};

template <typename addr_t>
bool DTracerWriter<addr_t>::open(const std::string &filename) {
    fstream.open(filename, std::ios::binary);
    if (!fstream.is_open()) return false;
    stream = &fstream;
    return true;
}

template<typename addr_t>
void DTracerWriter<addr_t>::open(std::ostream &ostream) {
    stream = &ostream;
}

template <typename addr_t>
bool DTracerWriter<addr_t>::close() {
    fstream.close();
    return !fstream.is_open();
}

template <typename addr_t>
void DTracerWriter<addr_t>::trace(const MemTracerEntry<addr_t> &entry) {
    stream->write((const char *)&entry.addr, sizeof(addr_t));
    char t = entry.memType;
    stream->write(&t, sizeof(char));
}

template <typename addr_t>
bool DTracerReader<addr_t>::open(const std::string &filename) {
    fstream.open(filename, std::ios::binary);
    if (!fstream.is_open()) return false;
    stream = &fstream;
    return true;
}

template <typename addr_t>
void DTracerReader<addr_t>::open(std::istream &istream) {
    stream = &istream;
}

template <typename addr_t>
bool DTracerReader<addr_t>::close() {
    fstream.close();
    return !fstream.is_open();
}

template <typename addr_t>
void DTracerReader<addr_t>::read_next() {
    addr_t addr;
    stream->read((char *)&addr, sizeof(addr));
    if (stream->eof()) {
        isEnd = true;
        return;
    }
    char t;
    stream->read(&t, sizeof(t));
    nextEntry = {addr, (MemType)t};
}

template <typename addr_t>
MemTracerEntry<addr_t> DTracerReader<addr_t>::begin() {
    addr_t addr;
    stream->read((char *)&addr, sizeof(addr));
    assert(!stream->fail());
    char t;
    stream->read((char *)&t, sizeof(t));
    assert(!stream->fail());
    read_next();
    return {addr, (MemType)t};
}

template <typename addr_t>
MemTracerEntry<addr_t> DTracerReader<addr_t>::next() {
    auto t = nextEntry;
    read_next();
    return t;
}

template <typename addr_t>
bool DTracerReader<addr_t>::is_end() const {
    return isEnd;
}

#endif
