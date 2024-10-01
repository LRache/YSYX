#ifndef __DTRACER_HPP__
#define __DTRACER_HPP__

#include "tracer.hpp"
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

#endif
