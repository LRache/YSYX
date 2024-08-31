#include <iostream>
#include <iomanip>
#include "itrace.h"
#include "sim.h"

void output_icache_sim(const SimResult &r, std::string name) {
    std::cout << "Sim Result of " << name << std::endl;
    std::cout 
    << std::setw(5) << "" << " | " 
    << std::setw(10) << "Count" << " | " 
    << std::setw( 7) << " " << " | " 
    << std::endl;

    uint64_t total = r.readHit + r.readMiss;
    std::cout 
    << std::setw( 5) << "hit" << " | "
    << std::setw(10) << r.readHit << " | "
    << std::setw( 6) << (double)r.readHit / total * 100 << "% | " 
    << std::endl;
    
    std::cout
    << std::setw( 5) << "miss" << " | "
    << std::setw(10) << r.readMiss << " | "
    << std::setw( 6) << (double)r.readMiss / total * 100 << "% | " 
    << std::endl;
}

int main(int argc, char **argv) {
    for (int i = 1; i < argc; i++) {
        ITracerReader reader;
        reader.open(argv[i]);
        FIFOCache cache(3, 0, 2);
        SimResult r = sim(cache, reader);
        output_icache_sim(r, "FIFLCache(3, 0, 2)");
    }
    return 0;
}
