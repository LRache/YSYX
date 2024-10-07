#include <iostream>
#include <string>
#include <getopt.h>

#include "itracer.hpp"
#include "dtracer.hpp"
#include "tracer.hpp"

using word_t = uint32_t;

void print_mem_trace(TracerReader<MemTracerEntry<word_t>> &reader, const std::string &filename) {
    if (!reader.open(filename)) {
        std::cerr << "Error to open file " << filename << std::endl;
        return;
    }
    std::cout << "Trace file " << filename << std::endl;
    std::cout << std::hex;
    for (auto entry = reader.begin(); !reader.is_end(); entry = reader.next()) {
        std::cout << (entry.memType == READ ? "READ " : "WRITE") << " 0x" << entry.addr << std::endl;
    }
    std::cout << std::dec;
}

void print_itrace(const std::string &filename) {
    ITracerReader<word_t> reader;
    print_mem_trace(reader, filename);
}

void print_dtrace(const std::string &filename) {
    DTracerReader<word_t> reader;
    print_mem_trace(reader, filename);
}

int main(int argc, char **argv)
{
    if (argc == 1) {
        printf("Usage: [--itrace] filename\n");
        return 1;
    }
    const struct option options[] = {
        {"itrace", required_argument, 0, 'i'},
        {"dtrace", required_argument, 0, 'd'},
        {0, 0, 0, 0},
    };

    char c;
    while ((c = getopt_long(argc, argv, "i:d:", options, NULL)) != -1)
    {
        switch (c) {
            case 'i': print_itrace(optarg); break;
            case 'd': print_dtrace(optarg); break;
        }
    }
    
    return 0;
}
