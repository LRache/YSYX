#include "trace.h"
#include "config.h"
#include "itracer.hpp"

#ifdef ITRACE

static ITracerWriter<word_t> writer;

bool itrace::open_file(const std::string &filename) {
    if (!config::itrace) return true;
    return writer.open(filename);
}

bool itrace::open_file() {
    return open_file(config::itraceOutputFileName);
}

void itrace::trace(word_t pc) {
    if (!config::itrace) return ;
    writer.trace(pc);
} 

bool itrace::close_file() {
    if (!config::itrace) return true;
    writer.end();
    return writer.close();
}

#else

bool itrace::open_file(const std::string &filename) { return true; }
void itrace::trace(word_t pc) {}
bool itrace::close_file() { return true; }

#endif
