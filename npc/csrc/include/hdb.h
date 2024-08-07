#ifndef __HDB_H__
#define __HDB_H__

#include <stdint.h>
#include <string>

#include "config.h"
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
} CPU;

extern CPU cpu;
extern VTop top;

void hdb_init(std::string imgPath="");
void hdb_step();
int hdb_run(uint64_t n = 0);

#endif