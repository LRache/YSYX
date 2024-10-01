#include "common.h"
#include "hdb.h"
#include "perf.h"
#include "trace.h"

extern "C" void interface_update_reset(uint8_t reset) {}

extern "C" void interface_ebreak() {
    hdb::ebreak();
}

extern "C" void interface_ivd_inst() {
    hdb::invalid_inst();
}

extern "C" void interface_update_pc(word_t pc) {
    hdb::set_pc(pc);
}

extern "C" void interface_update_inst(word_t inst) {
    hdb::set_inst(inst);
}

extern "C" void interface_update_done(bool done) {
    hdb::set_done(done);
}

extern "C" void interface_update_gpr(uint32_t addr, word_t data) {
    hdb::set_gpr(addr, data);
}

extern "C" void interface_update_csr(uint32_t addr, word_t data) {
    hdb::set_csr(addr, data);
}

extern "C" void interface_update_lsu_state(bool ren, bool wen, bool waiting, uint32_t addr) {
    dtrace::lsu_state_update(wen, waiting, addr);
    perf::lsu_state_update(ren, wen, waiting, addr);
}

extern "C" void interface_update_icache_mem_valid(bool valid) {
    ictrace::icache_mem_valid(valid);
    perf::icache_mem_valid_update(valid);
}

extern "C" void interface_update_icache_mem_start(bool start) {
    perf::icache_mem_start_update(start);
}

extern "C" void interface_update_idu_ready(bool ready) {
    ictrace::idu_ready_update(ready);
    perf::idu_ready_update(ready);
}

extern "C" void interface_update_exu_valid(bool valid) {
    perf::exu_valid_update(valid);
}

extern "C" void interface_update_icache_hit(bool hit) {
    ictrace::icache_hit(hit);
    perf::icache_is_hit_update(hit);
}

extern "C" void interface_update_icache_pc(word_t pc) {
    ictrace::icache_pc_update(pc);
    perf::icache_pc_update(pc);
}

extern "C" void interface_update_branch_predict_failed(bool failed) {
    perf::branch_predict_failed_update(failed);
}

extern "C" void interface_update_branch_predict_success(bool failed) {
    perf::branch_predict_success_update(failed);
}
