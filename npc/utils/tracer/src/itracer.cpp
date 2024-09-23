#include "itracer.hpp"

ITracerWriter::ITracerWriter() {
    this->isStart = false;
    this->turnCount = 0;
    this->outOfRange = false;
}

bool ITracerWriter::open(const std::string &filename) {
    this->fstream.open(filename, std::ios::out | std::ios::binary);
    if (this->fstream.is_open()) {
        this->stream = &this->fstream;
    }
    return this->fstream.is_open();
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
            }
        }
        this->pc = dnpc;
        this->turnCount ++;
    }
}

void ITracerWriter::end(word_t epc) {
    this->stream->write((char *)(&epc), sizeof(epc));
    if (this->fstream.is_open()) this->fstream.close();
}

bool ITracerReader::open(const std::string &filename) {
    this->fstream.open(filename, std::ios::in | std::ios::binary);
    if (this->fstream.is_open()) {
        this->stream = &this->fstream;
    }
    return this->fstream.is_open();
}

bool ITracerReader::close() {
    if (this->fstream.is_open()) this->fstream.close();
    return !this->fstream.is_open();
}

void ITracerReader::read_turn() {
    word_t pc;
    this->stream->read((char *)&pc, sizeof(pc));
    if (this->stream->eof()) {
        this->isEndTurn = true;
    } else {
        this->nextJumpPC = pc;
        this->stream->read((char *)&this->nextJumpDest, sizeof(this->nextJumpDest));
    }
}

word_t ITracerReader::begin() {
    this->stream->read((char *)&this->pc, sizeof(this->pc));
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
    word_t pc = this->pc;
    this->pc = npc;
    return pc;
}

bool ITracerReader::is_end() const {
    return this->isEnd;
}
