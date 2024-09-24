#ifndef __ITRACER_H__
#define __ITRACER_H__

#include "tracer.hpp"

#include <fstream>

#define ITRacerMaxTurn 4294967296L

template <typename addr_t>
class ITracerWriter : public TracerWriter<MemTracerEntry<addr_t>> {
private:
    bool isStart;
    bool outOfRange;
    addr_t pc;
    std::ostream *stream;
    std::ofstream fstream;
    uint64_t turnCount;
public:
    ITracerWriter();
    bool open(const std::string &filename) override;
    void open(std::ostream &stream) override;
    bool close() override;
    void trace(addr_t dnpc);
    void trace(const MemTracerEntry<addr_t> &dnpc) override;
    void end();
};

template <typename addr_t>
class ITracerReader : public TracerReader<MemTracerEntry<addr_t>> {
private:
    std::istream *stream;
    std::ifstream fstream;

    bool isEndTurn = false;
    bool isEnd = true;
    addr_t pc;
    addr_t nextJumpPC;
    addr_t nextJumpDest;
    void read_turn();
public:
    ITracerReader();
    bool open(const std::string &filename) override;
    void open(std::istream &stream) override;
    bool close() override;
    MemTracerEntry<addr_t> begin() override;
    MemTracerEntry<addr_t> next() override;
    bool is_end() const override;
};

template <typename addr_t>
ITracerWriter<addr_t>::ITracerWriter() : isStart(false), outOfRange(false), turnCount(0) {}

template <typename addr_t>
bool ITracerWriter<addr_t>::open(const std::string &filename) {
    this->fstream.open(filename, std::ios::out | std::ios::binary);
    if (this->fstream.is_open()) {
        this->stream = &this->fstream;
    }
    return this->fstream.is_open();
}

template <typename addr_t>
void ITracerWriter<addr_t>::open(std::ostream &stream) {
    this->stream = &stream;
}

template <typename addr_t>
bool ITracerWriter<addr_t>::close() {
    if (this->fstream.is_open()) this->fstream.close();
    return !this->fstream.is_open();
}

template <typename addr_t>
void ITracerWriter<addr_t>::trace(addr_t dnpc) {
    if (this->outOfRange) return ;
    
    if (!this->isStart) {
        this->isStart = true;
        this->stream->write((char *)&dnpc, sizeof(dnpc));
    } else {
        addr_t snpc = this->pc + 4;
        if (snpc != dnpc) {
            if (this->turnCount == ITRacerMaxTurn) {
                this->stream->write((char *)&this->pc, sizeof(this->pc));
                this->outOfRange = true;
            } else {
                this->stream->write((char *)&this->pc, sizeof(this->pc));
                this->stream->write((char *)&dnpc, sizeof(dnpc));
                this->turnCount ++;
            }
        }
    }
    this->pc = dnpc;
}

template <typename addr_t>
void ITracerWriter<addr_t>::trace(const MemTracerEntry<addr_t> &dnpc) {
    this->trace(dnpc.addr);
}

template <typename addr_t>
void ITracerWriter<addr_t>::end() {
    this->stream->write((char *)(&this->pc), sizeof(this->pc));
    if (this->fstream.is_open()) this->fstream.close();
}

template <typename addr_t>
ITracerReader<addr_t>::ITracerReader() : isEnd(false) {}

template <typename addr_t>
bool ITracerReader<addr_t>::open(const std::string &filename) {
    this->fstream.open(filename, std::ios::in | std::ios::binary);
    if (this->fstream.is_open()) {
        this->stream = &this->fstream;
    }
    return this->fstream.is_open();
}

template <typename addr_t>
void ITracerReader<addr_t>::open(std::istream &stream) {
    this->stream = &stream;
}

template <typename addr_t>
bool ITracerReader<addr_t>::close() {
    if (this->fstream.is_open()) this->fstream.close();
    return !this->fstream.is_open();
}

template <typename addr_t>
void ITracerReader<addr_t>::read_turn() {
    addr_t pc;
    this->stream->read((char *)&pc, sizeof(pc));
    this->nextJumpPC = pc;
    this->stream->read((char *)&pc, sizeof(pc));
    if (this->stream->eof()) {
        this->isEndTurn = true;
    } else {
        nextJumpDest = pc;
    }
}

template <typename addr_t>
MemTracerEntry<addr_t> ITracerReader<addr_t>::begin() {
    this->stream->read((char *)&this->pc, sizeof(this->pc));
    assert(!this->stream->fail());
    this->read_turn();
    return {this->pc, MemType::READ};
}

template <typename addr_t>
MemTracerEntry<addr_t> ITracerReader<addr_t>::next() {
    addr_t npc = pc + 4;
    if (this->pc == this->nextJumpPC) {
        npc = this->nextJumpDest;
        if (this->isEndTurn) {
            this->isEndTurn = false;
            this->isEnd = true;
        } else {
            this->read_turn();
        }
    }
    this->pc = npc;
    return {npc, MemType::READ};
}

template <typename addr_t>
bool ITracerReader<addr_t>::is_end() const {
    return this->isEnd;
}

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