#ifdef TOP_MEMORY

#include "memory.h"
#include "debug.h"
#include "VMemory/VMemory.h"

void test_mem() {
    VMemory top;
    uint32_t addr = MEM_BASE;
    for (int i = 1; i <= 1000; i++) {
        top.clk = 0;
        top.eval();

        top.addr = addr;
        top.din = i;
        top.wen = 1;
        top.len = 4;
        top.clk = 1;
        top.eval();
        Assert(mem_read(addr, 4) == i, "Memory check failed.");
        addr += 4;
    }
    top.wen = 0;

    addr = MEM_BASE;
    for (int i = 1; i <= 1000; i++) {
        top.clk = 0;
        top.eval();

        top.addr = addr;
        top.ren = 1;
        top.len = 4;
        top.clk = 1;
        top.eval();
        Assert(top.dout == i, "top.dout=" FMT_WORD ", i = %d", top.dout, i);
        addr += 4;
    }
}

#endif