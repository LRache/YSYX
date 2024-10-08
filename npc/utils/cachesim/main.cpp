#include <iostream>
#include <iomanip>
#include <string>
#include <vector>
#include <string.h>

#include "itracer.hpp"
#include "fifoCache.hpp"
#include "lruCache.hpp"
#include "sim.hpp"
#include "tempDecompress.hpp"

using word_t = uint32_t;

void output_icache_sim(const SimResult &r, std::string name, int e, int s, int b) {
    std::cout << std::fixed << std::setprecision(6);

    std::cout << name << "(" << e <<", " << s << ", " << b << ")" << std::endl;
    std::cout 
    << std::setw(5) << "" << " | " 
    << std::setw(10) << "Count" << " | " 
    << std::setw(11) << " " << " | " 
    << std::endl;

    uint64_t total = r.readHit + r.readMiss;
    std::cout 
    << std::setw( 5) << "hit" << " | "
    << std::setw(10) << r.readHit << " | "
    << std::setw(10) << (double)r.readHit / total * 100 << "% | " 
    << std::endl;
    
    std::cout
    << std::setw( 5) << "miss" << " | "
    << std::setw(10) << r.readMiss << " | "
    << std::setw(10) << (double)r.readMiss / total * 100 << "% | " 
    << std::endl;

    std::cout.unsetf(std::ios::fixed); 
    std::cout.unsetf(std::ios::floatfield);
}

void sim_raw_file(const std::string &filename, int e, int s, int b) {
    ITracerReader<word_t> reader0;
    reader0.open(filename);
    FIFOCache<word_t> fifo(e, s, b);
    SimResult r;
    r = sim(fifo, reader0);
    output_icache_sim(r, "FIFOCache", e, s, b);
    reader0.close();
    
    ITracerReader<word_t> reader1;
    reader1.open(filename);
    LRUCache<word_t> lru(e, s, b);
    r = sim(lru, reader1);
    output_icache_sim(r, "LRUCache", e, s, b);
    reader1.close();
}

bool zipMode = false;

void sim_file(const std::string &filename) {
    std::cout << "Simulating " << filename << std::endl;
    for (int e = 0; e < 3; e++) {
        for (int s = 0; s < 3; s++) {
            if (zipMode) {
                TempDecompressFile f(filename);
                if (f.is_failed()) {
                    std::cerr << "Failed to decompress file " << filename << std::endl;
                    return;
                }
                sim_raw_file(f.get_temp_filename(), e, s, 4);
            } else {
                sim_raw_file(filename, e, s, 4);
            }
        }
    }
}

int main(int argc, char **argv) {
    if (argc < 2) {
        std::cout << "Usage: " << argv[0] << " [--zip] <filename>" << std::endl;
        return 1;
    }
    std::vector<std::string> fileList;
    for (int i = 1; i < argc; i++) {
        if(strcmp(argv[i], "--zip") == 0) {
            zipMode = true;
        } else {
            fileList.push_back(argv[i]);
        }
    }
    for (const auto &filename : fileList) {
        sim_file(filename);
    }
    return 0;
}
