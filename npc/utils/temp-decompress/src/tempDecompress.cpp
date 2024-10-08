#include "tempDecompress.hpp"

#include <iostream>

static int run_command(const std::string &command) {
    // std::cout << "Run: " << command << std::endl;
    FILE *f = popen((command + " 2>/dev/null").c_str(), "r");
    // while (true) {
    //     char c = getc(f);
    //     if (feof(f)) break;
    //     std::cout << c;
    // }
    int s = pclose(f);
    return s;
}

template <typename T, typename... Args>
static int run_command(const std::string &command, T arg0, Args... args) {
    std::string newCommand = command + " " + arg0;
    return run_command(newCommand, args...);
}

TempDecompressFile::TempDecompressFile(const std::string &filename) {
    tempFilename = filename + ".out";
    failed = run_command("bunzip2", filename, "-f", "-k") != 0;
    if (failed) return;
}

TempDecompressFile::~TempDecompressFile() {
    run_command("rm", tempFilename);
}

std::string TempDecompressFile::get_temp_filename() const {
    return tempFilename;
}

bool TempDecompressFile::is_failed() const {
    return failed;
}
