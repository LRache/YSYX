#include "itrace.h"

#include <fstream>
#include <iostream>

ITrace::ITrace() {
    this->pc = 0;
}

ITrace::ITrace(const std::string &filename) {
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

void ITrace::start(word_t startPC) {
    this->startPC = startPC;
    this->pc = startPC;
    this->tracer.clear();
}

void ITrace::trace(word_t npc) {
    if (tracer.size() > ITRACE_LIMIT) return ;
    if (npc != this->pc + 4) {
        tracer.push_back({pc, npc});
        this->pc = npc;
    }
}

void ITrace::end() {
    this->endPC = this->pc;
}

void ITrace::dump_to_file(const std::string &filename) {
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

void ITrace::load_from_file(const std::string &filename) {
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

void ITrace::print() {
    std::cout << "START at pc=" << std::hex << this->startPC << std::endl;
}
