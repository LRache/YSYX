#ifndef __ITRACE_H__
#define __ITRACE_H__

#include "trace.h"

#include <vector>
#include <utility>
#include <string>

#define ITRACE_LIMIT 1000000000

class ITracer : public Tracer {
private:
    using pair = std::pair<word_t, word_t>;
    std::vector<pair> tracer;
    word_t pc;
    word_t startPC;
    word_t endPC;

    word_t iterPC;
    int iterIndex;
    bool iterIsEnd;

public:
    ITracer();
    ITracer(const std::string &filename);
    void start(word_t startPC);
    void trace(word_t pc);
    void end();
    void dump_to_file(const std::string &filename);
    void load_from_file(const std::string &filename);
    void print();
    
    void iter_init() override;
    word_t iter_next(Type *t) override;
    bool iter_is_end() override;
};

#endif
