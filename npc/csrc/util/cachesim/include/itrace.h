#ifndef __ITRACE_H__
#define __ITRACE_H__

#include <vector>
#include <utility>
#include <stdint.h>
#include <string>

#define ITRACE_LIMIT 1000000000

typedef uint32_t word_t;

class ITrace {
private:
    using pair = std::pair<word_t, word_t>;
    std::vector<pair> tracer;
    word_t pc;
    word_t startPC;
    word_t endPC;

public:
    ITrace();
    ITrace(const std::string &filename);
    void start(word_t startPC);
    void trace(word_t pc);
    void end();
    void dump_to_file(const std::string &filename);
    void load_from_file(const std::string &filename);
    void print();
};

#endif
