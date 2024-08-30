#include "itrace.h"

#include <fstream>
#include <iostream>
#include <iomanip>

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

void ITracer::start(word_t startPC) {
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

void ITracer::end() {
    this->endPC = this->pc;
}

void ITracer::dump_to_file(const std::string &filename) {
    std::ofstream f;
    f.open(filename, std::ios::binary);
    if (!f.is_open()) return ;
    f.write((const char *)&this->startPC, sizeof(word_t));
    for (auto &p : tracer) {
        f.write((const char *)&p.first,  sizeof(word_t));
        f.write((const char *)&p.second, sizeof(word_t));
    }
    f.write((const char *)&this->endPC, sizeof(word_t));
    f.close();
}

void ITracer::load_from_file(const std::string &filename) {
    std::ifstream f;
    f.open(filename, std::ios::binary);
    if (!f.is_open()) return ;
    f.read((char *)&this->startPC, sizeof(word_t));
    while (!f.eof()) {
        pair p;
        f.read((char *)&p.first, sizeof(word_t));
        f.read((char *)&p.second , sizeof(word_t));
        tracer.push_back(p);
    }
    f.read((char *)&this->endPC, sizeof(word_t));
}

void ITracer::print() {
    std::cout << "START at pc=" << FMT_WORD << this->startPC << std::endl;
    for (auto &p : tracer) {
        std::cout << "[" << FMT_WORD << p.first << "]" <<"Jump to " << FMT_WORD << p.second << std::endl;
    }
    std::cout << "END at pc=" << FMT_WORD << this->endPC << std::endl;
    std::cout << std::dec;
}

void ITracer::iter_init() {
    iterPC = this->startPC;
    iterIndex = 0;
    iterIsEnd = false;
}

word_t ITracer::iter_next(Type *t) {
    if (t != nullptr) *t = Type::READ;

    if (iterIndex == tracer.size() && iterPC == this->endPC) {
        this->iterIsEnd = true;
        return iterPC;
    }

    word_t npc = iterPC;
    if (iterPC == this->tracer[iterIndex].first) {
        iterPC = this->tracer[iterIndex].second;
        iterIndex++;
    } else {
        iterPC += 4;
    }
    return npc;
}

bool ITracer::iter_is_end() {
    return this->iterIsEnd;
}
