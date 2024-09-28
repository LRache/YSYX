#include <iostream>
#include <string>
#include <getopt.h>

#include "itracer.hpp"

using word_t = uint32_t;

void print_itrace(const std::string &filename) {
    ITracerReader<word_t> reader;
    if (!reader.open(filename)) {
        std::cerr << "Error to open file " << filename << std::endl;
        return;
    }
    std::cout << "Trace file " << filename << std::endl;
    std::cout << std::hex;
    for (auto entry = reader.begin(); !reader.is_end(); entry = reader.next()) {
        std::cout << "0x" << entry.addr << std::endl;
    }
    std::cout << std::dec;
}

int main(int argc, char **argv)
{
    if (argc == 1) {
        printf("Usage: [--itrace] filename\n");
        return 1;
    }
    const struct option options[] = {
        {"itrace", required_argument, 0, 'i'},
        {0, 0, 0, 0},
    };

    char c;
    while ((c = getopt_long(argc, argv, "i:", options, NULL)) != -1)
    {
        switch (c) {
            case 'i': print_itrace(optarg); break;
        }
    }
    
    return 0;
}
