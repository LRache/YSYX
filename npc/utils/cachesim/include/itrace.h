#pragma once

#include "trace.h"

#include <vector>
#include <utility>
#include <string>
#include <fstream>

#define ITRACE_LIMIT 1000000000

using ITracerPair = std::pair<word_t, word_t>;

class ITracerIterator {
    const std::vector<ITracerPair> *tracer;
    word_t pc;
    uint64_t index;
public:
    ITracerIterator(const std::vector<ITracerPair> *tracer, word_t pc, uint64_t index);
    ITracerIterator(const ITracerIterator &other);
    ITracerIterator(const ITracerIterator &&other);
    MemTracerAddr operator*() const;
    ITracerIterator& operator++();
    bool operator==(const ITracerIterator &other) const;
    bool operator!=(const ITracerIterator &other) const;
};

class ITracer : public Tracer<ITracerIterator> {
public:
    ITracer();
    ITracer(const std::string &filename);
    void start_trace(word_t startPC);
    void trace(word_t pc);
    void end_trace();
    void dump_to_file(const std::string &filename);
    void load_from_file(const std::string &filename);
    void print();
    
    ITracerIterator begin() const override;
    ITracerIterator end() const override;

private:
    std::vector<ITracerPair> tracer;
    word_t pc;
    word_t startPC;
    word_t endPC;
};

class ITracerReader : public MemTracerReader {
private:
    std::ifstream f;
    
    bool isEndTurn = false;
    bool isEnd = true;
    word_t pc;
    word_t nextJumpPC;
    word_t nextJumpDest;
    word_t endPC;
    void read_turn();
public:
    ~ITracerReader(); 
    void open(const std::string &filepath);
    
    MemTracerAddr next() override;
    bool is_end() const override;
    void close() override;
};
