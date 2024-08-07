#include <chrono>
#include <random>

uint64_t get_time() {
    std::chrono::high_resolution_clock::now();
}

std::mt19937 generator;

int randint(int start, int end) {
    return std::uniform_int_distribution<int> (start, end)(generator);
}
