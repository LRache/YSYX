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

void sim_file(const std::string &filename, int e, int s, int b) {
    ITracerReader reader;
    reader.open(filename);
    FIFOCache cache(e, s, b);
    SimResult r = sim(cache, reader);
    output_icache_sim(r, "FIFOCache", e, s, b);
    reader.close();
}

int main(int argc, char **argv) {
    // for (int i = 1; i < argc; i++) {
    //     std::cout << "SIM for " << argv[i] << std::endl;
    //     for (int j = 2; j < 6; j++) {
    //         for (int k = 0; k < 3; k++) {
    //             sim_file(argv[i], j, k, 4);
    //         }
    //     }
    // }
    sim_file(argv[1], 2, 0, 4);
    sim_file(argv[1], 3, 0, 2);
    // ITracer t;
    // t.start_trace(0);
    // for (int i = 4; i < 23 * 4; i += 4) {
    //     t.trace(i);
    // }
    // t.end_trace();
    // FIFOCache c(2, 0, 4);
    // auto r = sim(c, t);
    // output_icache_sim(r, "", 0, 0, 0);
    return 0;
}
