#include <iostream>
#include <string>
#include <iomanip>
#include <vector>
#include <string.h>

#include "itracer.hpp"
#include "branchsim.hpp"
#include "normalPredictor.hpp"
#include "tempDecompress.hpp"

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

bool zipMode = false;

void sim_raw_file(const std::string &filename) {
    std::cout << "Simulating " << filename << std::endl;
    ITracerReader<word_t> reader;
    if (!reader.open(filename)) {
        std::cerr << "Failed to open trace file" << std::endl;
        return;
    }
    NormalPredictor<word_t> p;
    auto r = branch_sim(p, reader);
    print_result(r, filename);
}

void sim_file(const std::string &filename) {
    if (zipMode) {
        TempDecompressFile temp(filename);
        if (temp.is_failed()) {
            std::cerr << "Failed to decompress file" << std::endl;
            return;
        } else {
            std::cout << "Decompressed to " << temp.get_temp_filename() << std::endl;
        }
        sim_raw_file(temp.get_temp_filename());
    } else {
        sim_raw_file(filename);
    }
    
}

int main(int argc, char **argv) {
    std::vector<std::string> fileList;
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--zip") == 0) {
            zipMode = true;
        } else {
            fileList.push_back(argv[i]);
        }
    }
    if (fileList.size() == 0) {
        std::cout << "Usage: ./branchsim [--zip] <trace_file>" << std::endl;
        return 1;
    }

    for (const auto &f : fileList) {
        sim_file(f);
    }
    return 0;
}
