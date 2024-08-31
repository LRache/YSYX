#include <iostream>
#include <iomanip>
#include "itrace.h"
#include "sim.h"

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

int main(int argc, char **argv) {
    for (int i = 1; i < argc; i++) {
        std::cout << "SIM for" << argv[i] << std::endl;
        for (int j = 2; j < 6; j++) {
            for (int k = 0; k < 3; k++) {
                ITracerReader reader;
                reader.open(argv[i]);
                FIFOCache cache(j, k, 2);
                SimResult r = sim(cache, reader);
                output_icache_sim(r, "FIFOCache", j, k, 2);
            }
        }
        // ITracerReader reader;
        // reader.open(argv[i]);
        // FIFOCache cache(3, 0, 2);
        // SimResult r = sim(cache, reader);
        // output_icache_sim(r, "FIFOCache", 3, 0, 2);
    }
    // ITracer t;
    // t.start_trace(0);
    // for (int i = 1; i < 4; i++) {
    //     t.trace(i * 4);
    // }
    // t.end_trace();
    // FIFOCache cache(3, 0, 2);
    // SimResult r = sim(cache, t);
    // output_icache_sim(r, "FIFOCache", 3, 0, 2);
    return 0;
}
