#include <iostream>
#include <string>
#include <iomanip>

#include "itracer.hpp"
#include "branchsim.hpp"
#include "normalPredictor.hpp"

#define STATISTIC_OUTPUT_INIT std::cout << std::fixed << std::setprecision(2);
#define STATISTIC_OUTPUT_DEINIT std::cout.unsetf(std::ios::fixed); std::cout.unsetf(std::ios::floatfield);

using word_t = uint32_t;

void print_result(const BranchSimResult &r, const std::string &name) {
    std::cout << "Sim result of " << name << std::endl;
    std::cout 
    << std::setw( 7) << " " << " | "
    << std::setw(10) << "Count" << " | "
    << std::setw( 8) << "Rate" << std::endl;
    
    double successRate = (double)r.success / (r.success + r.fail);
    STATISTIC_OUTPUT_INIT
    std::cout
    << std::setw( 7) << "Success" << " | "
    << std::setw(10) << r.success << " | "
    << std::setw( 8) << successRate * 100 << "%" << std::endl;
    std::cout
    << std::setw( 7) << "Fail" << " | "
    << std::setw(10) << r.fail << " | "
    << std::setw( 8) << (1-successRate) * 100 << "%" << std::endl;
    STATISTIC_OUTPUT_DEINIT
};

int main(int argc, char **argv) {
    if (argc < 2) {
        std::cout << "Usage: ./branchsim <trace_file>" << std::endl;
        return 1;
    }
    
    std::string trace_file(argv[1]);
    std::cout << "Trace file: " << trace_file << std::endl;
    std::cout << "Starting simulation..." << std::endl;
    ITracerReader<word_t> reader;
    if (!reader.open(trace_file)) {
        std::cerr << "Failed to open trace file" << std::endl;
        return 1;
    }
    NormalPredictor<word_t> p;
    auto r = branch_sim(p, reader);
    print_result(r, "Normal");
    return 0;
}
