#include <getopt.h>
#include <string>
#include <cstdlib>
#include <unistd.h>
#include <limits.h>
#include <iostream>

#include "debug.h"
#include "hdb.h"
#include "config.h"

void atexit() {
    hdb::end();
    Log("Bye~");
}

int main(int argc, char **argv) {
    Verilated::commandArgs(argc, argv);

    // char path[PATH_MAX];
    // ssize_t count = readlink("/proc/self/exe", path, PATH_MAX);
    // if (count == -1) {
    //     std::cerr << "无法获取路径" << std::endl;
    //     return 1;
    // }
    // path[count] = '\0'; // null terminate
    // std::cout << "可执行文件路径: " << path << std::endl;
    
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
    hdb::init(memImg, romImg, flashImg);
    std::atexit(atexit);
    int r = 0;
    r = hdb::run();
    return r;
}