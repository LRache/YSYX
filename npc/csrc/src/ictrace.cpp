#include "trace.h"
#include "config.h"
#include "ctracer.hpp"
#include "debug.h"

#ifdef ICTRACE

static ICTracerWriter<word_t> writer;
word_t pc;
bool iduReady = false;

bool ictrace::open_file(const std::string &filename) {
    if (!config::ictrace) return true;
    return writer.open(filename);
}

bool ictrace::open_file() {
    if (!config::ictrace) return true;
    return open_file(config::ictraceOutputFileName);
}

bool ictrace::close_file() {
    if (!config::ictrace) return true;
    Log("Close");
    writer.end();
    return writer.close();
}

void ictrace::idu_ready_update(bool ready) {
    iduReady = ready;
}

void ictrace::icache_hit(bool hit) {
    if (!config::ictrace) return;
    if (hit && iduReady) writer.trace(pc);
}

void ictrace::icache_mem_valid(bool valid) {
    if (!config::ictrace) return;
    if (valid) writer.trace(pc);
}

void ictrace::icache_pc_update(word_t p) {
    pc = p;
}

#else

bool ictrace::open_file(const std::string &filename) {
    return true;
}

bool ictrace::open_file() {
    return true;
}

bool ictrace::close_file() {
    return true;
}

#endif
