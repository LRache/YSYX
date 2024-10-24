#ifndef __HDB_H__
#define __HDB_H__

#include <stdint.h>

#include "common.h"
#include "VysyxSoCFull/VysyxSoCFull.h"

typedef struct
{
    word_t gpr[32];
    word_t pc;
    
    word_t mcause;
    word_t mepc;
    word_t mscratch;
    word_t mstatus;
    word_t mtvec;
    word_t satp;
    
    bool reset;
    uint64_t clockCount;

    // Debug
    bool running;
    bool done;
    uint64_t instCount;
    word_t inst;
    word_t lastInst;
    word_t lastPC;
} CPU;

extern VysyxSoCFull top;
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
    void set_reset(bool reset);
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