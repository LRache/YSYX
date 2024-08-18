#include <getopt.h>
#include <string>

#include "debug.h"
#include "hdb.h"

int main(int argc, char **argv) {
    Verilated::commandArgs(argc, argv);
    
    Log("Hello World!");

    struct option options[] = {
        {"mem",     required_argument, 0, 'm'},
        {"rom",     required_argument, 0, 'r'},
        {"flash",   required_argument, 0, 'f'},
        {0, 0, 0, 0}
    };
    
    std::string memImg = "";
    std::string romImg = "";
    std::string flashImg = "";
    int optionIndex = 0;
    int o;
    while ((o = getopt_long(argc, argv, "m:r:f:", options, &optionIndex)) != -1)
    {
        switch (o)
        {
            case 'm': memImg = optarg; break;
            case 'r': romImg = optarg; break;
            case 'f': flashImg = optarg; break;
            default: break;
        }
    }
    hdb::init(memImg, romImg, flashImg);
    int r = 0;
    r = hdb::run();
    return r;
}