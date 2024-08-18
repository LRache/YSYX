#include "hdb.h"
#include "debug.h"
#include "perf.h"

void perf::ifu_valid_update(bool v) {
    // Log("Ifu set read valid = %d at clock=%lu, pc=" FMT_WORD, v, cpu.clockCount, cpu.pc);
}

extern "C" void perf_ifu_valid_update(bool v) {
    perf::ifu_valid_update(v);
}

extern "C" void perf_lsu_wait_mem_start(bool t) {
    if (t) {
        Log("Perf LSU wait mem start at clock=%lu", cpu.clockCount);
    }
}

extern "C" void perf_lsu_mem_valid(bool t) {
    if (t) {
        Log("Perf LSU mem valid at clock=%lu", cpu.clockCount);
    }
}

