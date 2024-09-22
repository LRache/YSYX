#include <getopt.h>
#include <string>
#include <cstdlib>

#include "debug.h"
#include "hdb.h"
#include "config.h"

int main(int argc, char **argv) {
    Verilated::commandArgs(argc, argv);
    
    Log("Hello World!");

    struct option options[] = {
        {"mem"      ,required_argument, 0, 'm'},
        {"rom"      ,required_argument, 0, 'r'},
        {"flash"    ,required_argument, 0, 'f'},
        {"outputdir",required_argument, 0, 'o'},
        {"nvboard"  ,no_argument      , 0, 'n'},
        {"itrace"   ,no_argument      , 0, 'i'},
        {0, 0, 0, 0}
    };
    
    std::string memImg = "";
    std::string romImg = "";
    std::string flashImg = "";
    std::string outputdir = "./";
    int optionIndex = 0;
    int o;
    while ((o = getopt_long(argc, argv, "m:r:f:o:n", options, &optionIndex)) != -1)
    {
        switch (o)
        {
            case 'm': memImg = optarg; break;
            case 'r': romImg = optarg; break;
            case 'f': flashImg = optarg; break;
            case 'o': outputdir = optarg; break;
            case 'n': config::hasNVBoard = true; break;
            case 'i': config::itrace = true; break;
            default: break;
        }
    }
    hdb::init(memImg, romImg, flashImg, outputdir);
    std::atexit(hdb::end);
    int r = 0;
    r = hdb::run();
    return r;
}