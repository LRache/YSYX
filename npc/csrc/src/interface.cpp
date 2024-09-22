#include "common.h"
#include "hdb.h"

extern "C" void interface_update_reset(uint8_t reset) {
    
}

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
