#include "itracer.hpp"

#include <iostream>

template <typename addr_t>
ITracerWriter::ITracerWriter() : isStart(false), outOfRange(false), turnCount(0) {}

bool ITracerWriter::open(const std::string &filename) {
    this->fstream.open(filename, std::ios::out | std::ios::binary);
    if (this->fstream.is_open()) {
        this->stream = &this->fstream;
    }
    return this->fstream.is_open();
}

void ITracerWriter::open(std::ostream &stream) {
    this->stream = &stream;
}

bool ITracerWriter::close() {
    if (this->fstream.is_open()) this->fstream.close();
    return !this->fstream.is_open();
}

void ITracerWriter::trace(word_t dnpc) {
    if (this->outOfRange) return ;
    
    if (!this->isStart) {
        this->isStart = true;
        this->stream->write((char *)&dnpc, sizeof(dnpc));
    } else {
        word_t snpc = this->pc + 4;
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

void ITracerWriter::end() {
    this->stream->write((char *)(&this->pc), sizeof(this->pc));
    if (this->fstream.is_open()) this->fstream.close();
}

ITracerReader::ITracerReader() : isEnd(false) {}

bool ITracerReader::open(const std::string &filename) {
    this->fstream.open(filename, std::ios::in | std::ios::binary);
    if (this->fstream.is_open()) {
        this->stream = &this->fstream;
    }
    return this->fstream.is_open();
}

void ITracerReader::open(std::istream &stream) {
    this->stream = &stream;
}

bool ITracerReader::close() {
    if (this->fstream.is_open()) this->fstream.close();
    return !this->fstream.is_open();
}

void ITracerReader::read_turn() {
    word_t pc;
    this->stream->read((char *)&pc, sizeof(pc));
    this->nextJumpPC = pc;
    this->stream->read((char *)&pc, sizeof(pc));
    if (this->stream->eof()) {
        this->isEndTurn = true;
    } else {
        nextJumpDest = pc;
    }
}

word_t ITracerReader::begin() {
    this->stream->read((char *)&this->pc, sizeof(this->pc));
    assert(!this->stream->fail());
    this->read_turn();
    return this->pc;
}

word_t ITracerReader::next() {
    word_t npc = pc + 4;
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
    return npc;
}

bool ITracerReader::is_end() const {
    return this->isEnd;
}
