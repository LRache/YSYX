#include "itrace.h"

#include <fstream>
#include <iostream>
#include <iomanip>
#include <cstring>
#include <cerrno>
#include <stdexcept>
#include <cassert>

#define FMT_WORD "0x"<<std::hex << std::setfill('0') << std::setw(8)

ITracer::ITracer() {
    this->pc = 0;
}

ITracer::ITracer(const std::string &filename) {
    std::ifstream f;
    f.open(filename, std::ios::binary);
    if (!f.is_open()) return;
    f.read((char *)&this->startPC, sizeof(word_t));
    while (!f.eof()) {
        word_t first, second;
        f.read((char *)&first,  sizeof(word_t));
        f.read((char *)&second, sizeof(word_t));
        this->tracer.push_back({first, second});
    }
    f.close();
}

void ITracer::start_trace(word_t startPC) {
    this->startPC = startPC;
    this->pc = startPC;
    this->tracer.clear();
}

void ITracer::trace(word_t npc) {
    if (tracer.size() > ITRACE_LIMIT) return ;
    if (npc != this->pc + 4) {
        tracer.push_back({pc, npc});
    }
    this->pc = npc;
}

void ITracer::end_trace() {
    this->endPC = this->pc;
}

void ITracer::dump_to_file(const std::string &filename) {
    std::ofstream f;
    f.open(filename, std::ios::binary);
    if (!f.is_open()) return ;
    f.write((const char *)&this->startPC, sizeof(word_t));
    f.write((const char *)&this->endPC, sizeof(word_t));
    for (auto &p : tracer) {
        f.write((const char *)&p.first,  sizeof(word_t));
        f.write((const char *)&p.second, sizeof(word_t));
    }
    f.close();
}

void ITracer::load_from_file(const std::string &filename) {
    // std::ifstream f;
    // f.open(filename, std::ios::binary);
    // if (!f.is_open()) return ;
    // f.read((char *)&this->startPC, sizeof(word_t));
    // while (!f.eof()) {
    //     ITracerPair p;
    //     f.read((char *)&p.first, sizeof(word_t));
    //     f.read((char *)&p.second , sizeof(word_t));
    //     tracer.push_back(p);
    // }
    // f.read((char *)&this->endPC, sizeof(word_t));
}

void ITracer::print() {
    std::cout << "START at pc=" << FMT_WORD << this->startPC << std::endl;
    for (auto &p : tracer) {
        std::cout << "[" << FMT_WORD << p.first << "]" <<"Jump to " << FMT_WORD << p.second << std::endl;
    }
    std::cout << "END at pc=" << FMT_WORD << this->endPC << std::endl;
    std::cout << std::dec;
}

ITracerIterator::ITracerIterator(const std::vector<ITracerPair> *tracer, word_t pc, uint64_t index) : tracer(tracer), pc(pc), index(index)
{

}

ITracerIterator::ITracerIterator(const ITracerIterator &other) {
    this->tracer = other.tracer;
    this->pc = other.pc;
    this->index = other.index;
}

ITracerIterator::ITracerIterator(const ITracerIterator &&other) {
    this->tracer = other.tracer;
    this->pc = other.pc;
    this->index = other.index;
}

MemTracerAddr ITracerIterator::operator*() const {
    return {this->pc, MemType::READ};
}

ITracerIterator &ITracerIterator::operator++() {
    if (index != tracer->size()) {
        if (this->pc == tracer->at(index).first) {
            this->pc = tracer->at(index).second;
            index ++;
            return *this;
        }
    }
    this->pc += 4;
    return *this;
}

bool ITracerIterator::operator==(const ITracerIterator &other) const {
    return this->pc == other.pc && this->index == other.index;
}

bool ITracerIterator::operator!=(const ITracerIterator &other) const {
    return this->pc != other.pc || this->index != other.index;
}

ITracerIterator ITracer::begin() const {
    return {&tracer, startPC, 0};
}

ITracerIterator ITracer::end() const {
    return {&tracer, endPC + 4, tracer.size()};
}

void ITracerReader::open(const std::string &filepath) {
    f.open(filepath, std::ios::binary);
    if (f.is_open()) {
        isEnd = false;
        f.read((char *)&pc, sizeof(word_t));
        f.read((char *)&endPC, sizeof(word_t));
        // std::cout << std::hex << pc << " " << endPC << std::endl;
        read_turn();
    } else {
        throw std::runtime_error(std::string(std::strerror(errno)) + ": " + filepath);
    }
}

void ITracerReader::read_turn() {
    if (isEnd) return ;
    f.read((char *)&nextJumpPC, sizeof(word_t));
    f.read((char *)&nextJumpDest, sizeof(word_t));
    isEndTurn = f.eof();
}

MemTracerAddr ITracerReader::begin() {
    assert(!isEnd);
    return { pc, MemType::READ };
}

MemTracerAddr ITracerReader::next() {
    assert(!isEnd);
    
    if (isEndTurn && pc == endPC) {
        isEnd = true;
        return { pc, MemType::READ };
    }

    word_t npc;
    if (pc == nextJumpPC) {
        npc = nextJumpDest;
        read_turn();
    } else {
        npc = pc + 4;
    }
    word_t p = pc;
    pc = npc;
    return { p, MemType::READ };
}

bool ITracerReader::is_end() const {
    return isEnd;
}

void ITracerReader::close() {
    f.close();
}

ITracerReader::~ITracerReader() {
    f.close();
}
