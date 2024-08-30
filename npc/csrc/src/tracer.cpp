#include "hdb.h"
#include "config.h"
#include "itrace.h"

#ifdef ITRACE

static ITrace itracer;

void itrace::start(word_t pc) {
    itracer.start(pc);
}

void itrace::trace(word_t pc) {
    itracer.trace(pc);
} 

void itrace::end() {
    itracer.end();
}

void itrace::dump_to_file(const std::string &filename) {
    itracer.dump_to_file(filename);
}

void itrace::print() {
    itracer.print();
}

#else

void itrace::start(word_t pc) {}
void itrace::trace(word_t pc) {}
void itrace::end() {}
void itrace::dump_to_file(const std::string &filename) {}
void itrace::print() {}

#endif
