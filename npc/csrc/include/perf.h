#ifndef __PERF_H__
#define __PERF_H__

#include "common.h"

namespace perf {
    void init();
    void statistic();
    void lsu_statistic();
    void icache_statistic();
    void branch_predictor_statistic();

    void lsu_state_update(bool ren, bool wen, bool waiting, addr_t addr);
    
    void idu_ready_update(bool ready);
    void exu_valid_update(bool valid);
    
    void icache_mem_valid_update(bool valid);
    void icache_mem_start_update(bool start);
    void icache_is_hit_update(bool isHit);
    void icache_pc_update(word_t pc);
    void branch_predict_failed_update(bool failed);
    void branch_predict_success_update(bool failed);
}

#endif
