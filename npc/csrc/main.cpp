#include <getopt.h>
#include <string>
#include <iostream>

#include "debug.h"
#include "hdb.h"

int main(int argc, char **argv) {
    Verilated::commandArgs(argc, argv);
    
    Log("Hello World!");
    std::string imgPath = "";
    int o;
    while ((o = getopt(argc, argv, "i:")) != -1)
    {
        switch (o)
        {
            case 'i': imgPath = optarg; break;
            default: break;
        }
    }
    hdb_init(imgPath);
    int r = 0;
    r = hdb_run();
    return r;
}