#include <getopt.h>
#include <unistd.h>

#include "debug.h"
#include "hdb.h"
#include "config.h"

void atexit() {
    hdb::end();
    Log("Bye~");
}

void parse_args(int argc, char **argv) {
    struct option options[] = {
        {"rom"      , required_argument, 0, 'r'},
        {"flash"    , required_argument, 0, 'f'},
        {"nvboard"  , no_argument      , 0, 'n'},
        {"itrace"   , required_argument, 0, 'i'},
        {"ictrace"  , required_argument, 0, 'c'},
        {"dtrace"   , required_argument, 0, 'd'},
        {"zip"      , no_argument      , 0, 'z'},
        {"nodifftest", no_argument     , 0, 'o'},
        {0, 0, 0, 0}
    };

    int o;
    while ((o = getopt_long(argc, argv, "m:r:o:i:n", options, NULL)) != -1) {
        switch (o) {
            case 'r': 
                config::romImgFileName = optarg; 
                config::loadRom = true;
                break;
            case 'f': 
                config::flashImgFileName = optarg;
                config::loadFlash = true;
                break;
            case 'n': 
                config::hasNVBoard = true; 
                break;
            case 'i': 
                config::itrace = true; 
                config::itraceOutputFileName = optarg;
                break;
            case 'c':
                config::ictrace = true;
                config::ictraceOutputFileName = optarg;
                break;
            case 'd':
                config::dtrace = true;
                config::dtraceOutputFileName = optarg;
                break;
            case 'o':
                config::hasDifftest = false;
                break;
            case 'z':
                config::zip = true;
                break;
        }
    }
}

int main(int argc, char **argv) {
    Log("Hello World!");
    Verilated::commandArgs(argc, argv);
    parse_args(argc, argv);

    std::atexit(atexit);
    
    hdb::init();
    int r = 0;
    r = hdb::run();
    return r;
}
