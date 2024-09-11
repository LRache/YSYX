#ifndef __HDB_H__
#define __HDB_H__

#include <stdint.h>
#include <string>

#include "config.h"
#include "common.h"
// #include "VTop/VTop.h"
#include "VysyxSoCFull/VysyxSoCFull.h"

typedef struct
{
    uint32_t gpr[32];
    uint32_t pc;
    
    uint32_t mcause;
    uint32_t mepc;
    uint32_t mscratch;
    uint32_t mstatus;
    uint32_t mtvec;
    uint32_t satp;
    
    bool running;
    bool valid;
    uint32_t inst;
    uint64_t clockCount;
    uint64_t instCount;
} CPU;

extern VTop top;
extern CPU cpu;

namespace hdb
{
    void init(
        const std::string &memImgPath="", const std::string &romImgPath="", 
        const std::string &flashImgPath="", const std::string &outputDir="./"
        );
    void step();
    int run(uint64_t n = 0);
    void end();
    
    extern std::string outputDir;
} // namespace hdb

namespace nvboard
{
    void init();
    void update();
    void quit();
} // namespace nvboard

namespace itrace
{
    void start(word_t pc);
    void trace(word_t pc);
    void end();
    void dump_to_file(const std::string &filename);
    void print();
    void sim_cache();
} // namespace tracer


#endif