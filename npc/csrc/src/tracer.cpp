#include "hdb.h"
#include "config.h"
#include "itrace.h"
#include "cache.h"

#include <iostream>
#include <iomanip>

#ifdef ITRACE

static ITracer itracer;

void itrace::start(word_t pc) {
    itracer.start_trace(pc);
}

void itrace::trace(word_t pc) {
    itracer.trace(pc);
} 

void itrace::end() {
    itracer.end_trace();
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
    std::cout << "Result of CacheSim" << std::endl;
    std::cout 
    << std::setw(5) << "" << " | " 
    << std::setw(10) << "Count" << " | " 
    << std::setw( 7) << " " << " | " 
    << std::endl;

    uint64_t total = r.readHit + r.readMiss;
    std::cout 
    << std::setw( 5) << "hit" << " | "
    << std::setw(10) << r.readHit << " | "
    << std::setw( 6) << (double)r.readHit / total * 100 << "% | " 
    << std::endl;
    std::cout
    << std::setw( 5) << "miss" << " | "
    << std::setw(10) << r.readMiss << " | "
    << std::setw( 6) << (double)r.readMiss / total * 100 << "% | " 
    << std::endl;
    std::cout << std::endl;
}

#else

void itrace::start(word_t pc) {}
void itrace::trace(word_t pc) {}
void itrace::end() {}
void itrace::dump_to_file(const std::string &filename) {}
void itrace::print() {}

#endif
