#ifndef __ITRACER_H__
#define __ITRACER_H__

#include "tracer.hpp"

#include <fstream>

#define ITRacerMaxTurn 4294967296L

typedef uint32_t word_t;

class ITracerWriter : public TracerWriter<word_t, void> {
private:
    bool isStart;
    bool outOfRange;
    word_t pc;
    std::ostream *stream;
    std::ofstream fstream;
    uint64_t turnCount;
public:
    ITracerWriter();
    bool open(const std::string &filename) override;
    bool close() override;
    void trace(word_t dnpc) override;
    void end(word_t epc);
};

class ITracerReader : public TracerReader<word_t, void> {
private:
    std::istream *stream;
    std::ifstream fstream;

    bool isEndTurn = false;
    bool isEnd = true;
    word_t pc;
    word_t nextJumpPC;
    word_t nextJumpDest;
    void read_turn();
public:
    bool open(const std::string &filename) override;
    bool close() override;
    word_t begin() override;
    word_t next() override;
    bool is_end() const override;
};

// using ITracerPair = std::pair<word_t, word_t>;

// class ITracerIterator {
//     const std::vector<ITracerPair> *tracer;
//     word_t pc;
//     uint64_t index;
// public:
//     ITracerIterator(const std::vector<ITracerPair> *tracer, word_t pc, uint64_t index);
//     ITracerIterator(const ITracerIterator &other);
//     ITracerIterator(const ITracerIterator &&other);
//     MemTracerAddr operator*() const;
//     ITracerIterator& operator++();
//     bool operator==(const ITracerIterator &other) const;
//     bool operator!=(const ITracerIterator &other) const;
// };

// class ITracer : public Tracer<ITracerIterator> {
// public:
//     ITracer();
//     ITracer(const std::string &filename);
//     void start_trace(word_t startPC);
//     void trace(word_t pc);
//     void end_trace();
//     void dump_to_file(const std::string &filename);
//     void load_from_file(const std::string &filename);
//     void print();
    
//     ITracerIterator begin() const override;
//     ITracerIterator end() const override;

// private:
//     std::vector<ITracerPair> tracer;
//     word_t pc;
//     word_t startPC;
//     word_t endPC;
// };

#endif