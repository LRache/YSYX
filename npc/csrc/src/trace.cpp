#include "trace.h"
#include "debug.h"
#include "config.h"
#include "utils.hpp"
#include <cstdio>
#include <string>

void trace::open() {
    Assert(itrace::open_file(), "Failed to open itrace file: %s.", config::itraceOutputFileName.c_str());  
    Assert(ictrace::open_file(), "Failed to open citrace file: %s.", config::itraceOutputFileName.c_str());
    Assert(dtrace::open_file(), "Failed to open dtrace file: %s", config::dtraceOutputFileName.c_str());
}

void trace::close() {
    Assert( itrace::close_file(), "Failed to close itrace file." );
    Assert(ictrace::close_file(), "Failed to close ictrace file.");
    Assert( dtrace::close_file(), "Failed to close dtrace file." );
    if (config::zip) {
        if (config::itrace) {
            run_command("bzip2", "-f", config::itraceOutputFileName);
            run_command("mv", config::itraceOutputFileName + ".bz2", config::itraceOutputFileName);
        }
        if (config::ictrace) {
            run_command("bzip2", "-f", config::ictraceOutputFileName);
            run_command("mv", config::ictraceOutputFileName + ".bz2", config::ictraceOutputFileName);
        }
        if (config::dtrace) {
            run_command("bzip2", "-f", config::dtraceOutputFileName);
            run_command("mv", config::dtraceOutputFileName + ".bz2", config::dtraceOutputFileName);
        }
    }
}
