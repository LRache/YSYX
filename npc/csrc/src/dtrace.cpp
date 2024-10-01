#include "common.h"
#include "config.h"
#include "trace.h"
#include "dtracer.hpp"
#include "tracer.hpp"

#include <string>

#ifdef DTRACE

static DTracerWriter<word_t> writer;

bool dtrace::open_file(const std::string &filename) {
    if (!config::dtrace) return true;
    return writer.open(filename);
}

bool dtrace::open_file() {
    if (!config::dtrace) return true;
    return open_file(config::dtraceOutputFileName);
}

bool dtrace::close_file() {
    if (!config::dtrace) return true;
    return writer.close();
}

void dtrace::trace(word_t addr, bool isWrite) {
    if (!config::dtrace) return ;
    MemType t = isWrite ? WRITE : READ;
    writer.trace({addr, t});
}

static bool isWrite;
static bool isWaiting;
static bool addr;
void dtrace::lsu_state_update(bool wen, bool waiting, addr_t a) {
    if (!config::dtrace) return;
    if (waiting) {
        isWaiting = true;
        isWrite = wen;
        addr = a;
    } else {
        isWaiting = false;
        trace(addr, isWrite);
    }
}

#else

bool dtrace::open_file(const std::string &filename) { return true; }

bool dtrace::open_file() { return true; }

bool dtrace::close_file() { return true; }

void dtrace::trace(word_t addr, bool isWrite) {}

#endif
