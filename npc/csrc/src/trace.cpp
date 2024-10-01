#include "trace.h"
#include "debug.h"
#include "config.h"

void trace::open() {
    Assert( itrace::open_file(), "Failed to open itrace file: %s.", config::itraceOutputFileName.c_str());
    Assert(ictrace::open_file(), "Failed to open citrace file: %s.", config::itraceOutputFileName.c_str());
}

void trace::close() {
    Assert( itrace::close_file(), "Failed to close itrace file.");
    Assert(ictrace::close_file(), "Failed to close ictrace file.");
}
