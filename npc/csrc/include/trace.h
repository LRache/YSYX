#ifndef __TRACE_H__
#define __TRACE_H__

#include <string>
#include "common.h"

namespace trace
{
    void open();
    void close();
} // namespace trace


namespace itrace
{
    bool open_file(const std::string &filename);
    bool open_file();
    void trace(word_t pc);
    bool close_file();
} // namespace itrace

namespace ictrace
{
    bool open_file(const std::string &filename);
    bool open_file();
    bool close_file();
    void idu_ready_update(bool ready);
    void icache_hit(bool hit);
    void icache_mem_valid(bool valid);
    void icache_pc_update(word_t pc);
} // namespace ctrace

namespace dtrace {
    bool open_file(const std::string &filename);
    bool open_file();
    bool close_file();
    void trace(word_t addr, bool isWrite);
    void lsu_state_update(bool wen, bool waiting, addr_t addr);
} // namespace dtrace


#endif