#ifndef __UTILS_H__
#define __UTILS_H__

#include <string>

int randint(int start, int end);
std::string us_to_text(uint64_t t);

void run_command(const std::string &command);

template <typename T, typename... Args>
void run_command(const std::string &command, T arg0, Args... args) {
    std::string newCommand = command + " " + arg0;
    run_command(newCommand, args...);
}


#endif
