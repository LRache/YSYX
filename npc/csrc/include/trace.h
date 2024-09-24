#ifndef __TRACE_H__
#define __TRACE_H__

#include <string>
#include "common.h"

namespace itrace
{
    bool open_file(const std::string &filename);
    bool open_file();
    void trace(word_t pc);
    bool close_file();
} // namespace itrace

#endif