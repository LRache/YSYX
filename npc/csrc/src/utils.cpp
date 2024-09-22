#include <string>
#include <random>

static std::mt19937 generator;

int randint(int start, int end) {
    return std::uniform_int_distribution<int> (start, end)(generator);
}

std::string us_to_text(uint64_t t) {
    std::string units[] = {"h", "m", "s", "ms", "us"};
    uint64_t r[5] = {};
    r[4] = t % 1000; // us
    t /= 1000;
    r[3] = t % 1000; // ms
    t /= 1000;
    r[2] = t % 60; // s
    t /= 60;
    r[1] = t % 60; // m
    t /= 60;
    r[0] = t; // h
    int i = 0;
    for (; i < 5; i++) {
        if (r[i] != 0) break;
    }
    if (i == 5) return "0 us";
    std::string result;
    for (int j = i; j < 5; j++) {
        result += std::to_string(r[j]) + units[j];
    }
    return result;
}

std::string number_split(uint64_t number) {
    std::string result;
    bool f = true;
    while (number) {
        uint64_t t = number % 1000;
        if (t >= 100) {
            result += std::to_string(t);
        } else if (t >= 10) {
            result += "0" + std::to_string(t);
        } else {
            result += "00" + std::to_string(t);
        }
        if (f) f = false;
        else result += ",";
    }
    return result;
}
