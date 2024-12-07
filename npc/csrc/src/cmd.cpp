#include "common.h"
#include "hdb.h"

#include <iostream>
#include <string>
#include <map>
#include <utility>

typedef int (*CmdFun)();

std::map<std::string, CmdFun> cmdMap;
bool running = true;

int set_breakpoint() {
    word_t pc;
    std::cin >> std::hex >> pc;
    hdb::add_breakpoint(pc);
    std::cout << "Create breakpoint at 0x" << std::hex << pc << std::endl;
    return 0;
}

int cmd_run() {
    hdb::run();
    return 0;
}

int quit() {
    running = false;
    return 0;
}

int cmd_step() {
    int n;
    std::cin >> n;
    cpu.running = true;
    for (int i = 0; i < n; i++) hdb::step();
    return 0;
}

void hdb::dbg_init() {
    cmdMap.insert(std::make_pair("b", set_breakpoint));
    cmdMap.insert(std::make_pair("break", set_breakpoint));
    cmdMap.insert(std::make_pair("q", quit));
    cmdMap.insert(std::make_pair("quit", quit));
    cmdMap.insert(std::make_pair("r", cmd_run));
    cmdMap.insert(std::make_pair("run", cmd_run));
    cmdMap.insert(std::make_pair("s", cmd_step));
    cmdMap.insert(std::make_pair("step", cmd_step));
}

void hdb::start_dbg() {
    running = true;
    while (running) {
        std::cout << "(hdb) ";
        std::string cmdName;
        std::cin >> cmdName;
        auto iter = cmdMap.find(cmdName);
        if (iter == cmdMap.end()) {
            std::cout << "Unknown command: " << cmdName << std::endl;
            continue;
        }
        CmdFun fun = iter->second;
        int r = fun();
        if (r != 0) {
            std::cout << "Command Failed" << std::endl;
        }
    }
}
