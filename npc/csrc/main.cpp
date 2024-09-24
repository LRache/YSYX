#include <getopt.h>
#include <string>
#include <cstdlib>

#include "debug.h"
#include "hdb.h"
#include "config.h"

void atexit() {
    hdb::end();
    Log("Bye~");
}

int main(int argc, char **argv) {
    Verilated::commandArgs(argc, argv);
    
    Log("Hello World!");

    struct option options[] = {
        {"mem"      ,required_argument, 0, 'm'},
        {"rom"      ,required_argument, 0, 'r'},
        {"flash"    ,required_argument, 0, 'f'},
        {"nvboard"  ,no_argument      , 0, 'n'},
        {"itrace"   ,required_argument, 0, 'i'},
        {0, 0, 0, 0}
    };
    
    std::string memImg = "";
    std::string romImg = "";
    std::string flashImg = "";
    int optionIndex = 0;
    int o;
    while ((o = getopt_long(argc, argv, "m:r:o:i:n", options, &optionIndex)) != -1)
    {
        switch (o)
        {
            case 'm': 
                memImg = optarg; 
                break;
            case 'r': 
                romImg = optarg; 
                break;
            case 'f': 
                flashImg = optarg; 
                break;
            case 'n': 
                config::hasNVBoard = true; 
                break;
            case 'i': 
                config::itrace = true; 
                config::itraceOutputFileName = optarg;
                break;
            default: break;
        }
    }
    assert(config::itrace);
    hdb::init(memImg, romImg, flashImg);
    std::atexit(atexit);
    int r = 0;
    r = hdb::run();
    return r;
}