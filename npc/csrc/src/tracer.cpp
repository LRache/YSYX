#include "hdb.h"
#include "config.h"
#include "itrace.h"
#include "cache.h"

#include <iostream>

#ifdef ITRACE

static ITracer itracer;

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

void itrace::sim_cache() {
    Cache cache(3, 0, 2);
    auto r = cache.sim(itracer);
    std::cout << r.readHit << std::endl;
    std::cout << r.readMiss << std::endl;
}

#else

void itrace::start(word_t pc) {}
void itrace::trace(word_t pc) {}
void itrace::end() {}
void itrace::dump_to_file(const std::string &filename) {}
void itrace::print() {}

#endif
