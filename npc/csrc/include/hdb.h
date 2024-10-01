#ifndef __HDB_H__
#define __HDB_H__

#include <stdint.h>
#include <string>

#include "config.h"
#include "common.h"
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
    bool done;
    uint32_t inst;
    uint64_t clockCount;
    uint64_t instCount;
} CPU;

extern VTop top;
extern CPU cpu;

namespace hdb
{
    void init();
    void step();
    int run(uint64_t n = 0);
    void end();

    void ebreak();
    void invalid_inst();
    void set_pc(word_t pc);
    void set_inst(word_t inst);
    void set_done(bool done);
    void set_gpr(uint32_t addr, word_t data);
    void set_csr(uint32_t addr, word_t data);
} // namespace hdb

namespace nvboard
{
    void init();
    void update();
    void quit();
} // namespace nvboard

#endif