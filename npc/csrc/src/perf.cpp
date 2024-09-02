#include <iostream>
#include <iomanip>

#include "hdb.h"
#include "debug.h"
#include "perf.h"
#include "memory.h"

#define STATISTIC_OUTPUT_INIT std::cout << std::fixed << std::setprecision(2);
#define STATISTIC_OUTPUT_DEINIT std::cout.unsetf(std::ios::fixed); std::cout.unsetf(std::ios::floatfield);

struct Counter {
    uint64_t count;
    uint64_t clockCount;
    double average() const {
        if (count == 0) return 0;
        return (double)clockCount / count;
    }

    void pref_count(uint64_t c) {
        count ++;
        clockCount += c;
    }
};

static struct {
    Counter flash;
    Counter sdram;
    Counter other;

    uint64_t start = 0;
} ifu;

static void ifu_valid_update(bool v) {
    if (v) {
        // Log("IFU valid at pc=" FMT_WORD " " FMT_WORD, cpu.pc, cpu.inst);
        uint64_t clockCount = cpu.clockCount - ifu.start;
        if (in_flash(cpu.pc)) ifu.flash.pref_count(clockCount);
        else if (in_sdram(cpu.pc)) ifu.sdram.pref_count(clockCount);
        else ifu.other.pref_count(clockCount);
    } else {
        ifu.start = cpu.clockCount;
    }
}

static inline void print_ifu_statistic(const char *name, const Counter &c) {
    std::cout 
    << std::setw( 6) << name << " | "
    << std::setw(12) << c.clockCount << " | " 
    << std::setw(10) << c.count << " | " 
    << std::setw( 8) << c.average() << " | "
    << std::setw( 5) << (double)c.clockCount / cpu.clockCount * 100 << "%" << std::endl;
}

void perf::ifu_statistic() {
    std::cout << "Performance Statistic of IFU" << std::endl;
    std::cout 
    << std::setw( 6) << "Source" << " | "
    << std::setw(12) << "Clock" << " | " 
    << std::setw(10) << "Count" << " | " 
    << std::setw( 8) << "Average" << " | " << std::endl;
    
    STATISTIC_OUTPUT_INIT;
    print_ifu_statistic("flash", ifu.flash);
    print_ifu_statistic("sdram", ifu.sdram);
    print_ifu_statistic("other", ifu.other);
    STATISTIC_OUTPUT_DEINIT;
}

static struct {
    Counter hit;
    Counter miss;

    bool isHit = false;
    bool started = false;
    uint64_t start;
} icache;

static void icache_valid_update(bool valid) {
    if (valid) {
        uint64_t clockCount = cpu.clockCount - icache.start;
        icache.miss.pref_count(clockCount);
        // icache.isHit = true;
    }
}

static void icache_start_update(bool start) {
    if (start) {
        icache.start = cpu.clockCount;
    }
}

static void icache_is_hit_update(bool isHit) {
    // icache.isHit = isHit;
    if (isHit) {
        // icache.hit.pref_count(0);
        Log("Hit at clock=%lu", cpu.clockCount);
    }
}

void perf::icache_statistic() {
    std::cout << "Performance Statistic of ICache" << std::endl;
    std::cout << std::setw(5) << "" << " | " 
    << std::setw(10) << "Count" << " | " 
    << std::setw( 7) << " " << " | " 
    << std::endl;
    
    uint64_t total = icache.hit.count + icache.miss.count;
    uint64_t totalClk = icache.hit.clockCount + icache.miss.clockCount;
    STATISTIC_OUTPUT_INIT;
    std::cout 
    << std::setw( 5) << "hit" << " | "
    << std::setw(10) << icache.hit.count << " | "
    << std::setw( 6) << (double)icache.hit.count / total * 100 << "% | " 
    // << std::setw(12) << icache.hit.clockCount << " | "
    // << std::setw( 6) << (double)icache.hit.clockCount / totalClk * 100 << "%" 
    << std::endl;

    std::cout 
    << std::setw( 5) << "miss" << " | "
    << std::setw(10) << icache.miss.count << " | "
    << std::setw( 6) << (double)icache.miss.count / total * 100 << "% | "
    // << std::setw(12) << icache.miss.clockCount << " | "
    // << std::setw( 6) << (double)icache.miss.clockCount / totalClk * 100 << "%" 
    << std::endl;

    std::cout 
    << std::setw( 5) << "Total" << " | "
    << std::setw(10) << total << " | " 
    << std::setw( 7) << "" << " | "
    << std::setw(12) << totalClk << " | "
    << std::endl;

    STATISTIC_OUTPUT_DEINIT;

    double amat = totalClk + (1 - (double) icache.miss.clockCount / totalClk) * icache.miss.clockCount;
    std::cout << "AMAT=" << std::fixed << amat << std::endl;
}

static struct {
    Counter flashRead;
    Counter sramRead;
    Counter sramWrite;
    Counter sdramRead;
    Counter sdramWrite;
    Counter otherRead;
    Counter otherWrite;
    Counter unexpRead;
    Counter unexpWrite;

    bool ren;
    bool wen;
    bool isWaiting = false;
    addr_t addr;
    uint64_t start;
} lsu;

static void lsu_state_update(bool ren, bool wen, bool waiting, addr_t addr) {
    if (waiting) {
        lsu.start = cpu.clockCount;
        lsu.isWaiting = true;
        lsu.wen = wen;
        lsu.ren = ren;
        lsu.addr = addr;
        // Log(FMT_WORD " " FMT_WORD, cpu.pc, lsu.addr);
    } else if (lsu.isWaiting) {
        lsu.isWaiting = false;
        uint64_t clockCount = cpu.clockCount - lsu.start;
        if (clockCount > 5000) {
            if (lsu.ren) lsu.unexpRead.pref_count(clockCount);
            else if (lsu.wen) lsu.unexpWrite.pref_count(clockCount);
        } else if (in_flash(lsu.addr)) {
            if (lsu.ren) lsu.flashRead.pref_count(clockCount);
        } else if (in_sram(lsu.addr)) {
            if (lsu.ren) lsu.sramRead.pref_count(clockCount);
            else if (lsu.wen) lsu.sramWrite.pref_count(clockCount);
        } else if (in_sdram(lsu.addr)) {
            if (lsu.ren) lsu.sdramRead.pref_count(clockCount);
            else if (lsu.wen) lsu.sdramWrite.pref_count(clockCount);
        } else {
            if (lsu.ren) lsu.otherRead.pref_count(clockCount);
            else if (lsu.wen) lsu.otherWrite.pref_count(clockCount);
        }
    }
}

static void print_lsu_statistic(const char *name, const Counter &c, bool is_read) {
    std::cout
    << std::setw( 6) << name << " | " 
    << (is_read ? " read" : "write") << " | "
    << std::setw(12) << c.clockCount << " | " 
    << std::setw(10) << c.count << " | " 
    << std::setw( 8) << c.average() << " | " 
    << std::setw( 5) << (double)c.clockCount / cpu.clockCount * 100 << "%" << std::endl;
}

void perf::lsu_statistic() {
    std::cout << "Performance Statistic of LSU" << std::endl;
    std::cout
    << std::setw( 6) << "Source" << " | "
    << " Type" << " | "
    << std::setw(12) << "Clock" << " | " 
    << std::setw(10) << "Count" << " | " 
    << std::setw( 8) << "Average" << std::endl;

    STATISTIC_OUTPUT_INIT;
    print_lsu_statistic("flash", lsu.flashRead, true);
    print_lsu_statistic("sram", lsu.sramRead, true);
    print_lsu_statistic("sram", lsu.sramWrite, false);
    print_lsu_statistic("sdram", lsu.sdramRead, true);
    print_lsu_statistic("sdram", lsu.sdramWrite, false);
    print_lsu_statistic("other", lsu.otherRead, true);
    print_lsu_statistic("other", lsu.otherWrite, false);
    print_lsu_statistic("unexp", lsu.unexpRead, true);
    print_lsu_statistic("unexp", lsu.unexpWrite, false);
    STATISTIC_OUTPUT_DEINIT;
}

void perf::init() {
    ifu.start = cpu.clockCount;
    icache.start = cpu.clockCount;
    lsu.start = cpu.clockCount;
}

void perf::statistic() {
    ifu_statistic();
    std::cout << std::endl;
    icache_statistic();
    std::cout << std::endl;
    lsu_statistic();
}

extern "C" void perf_ifu_valid_update(bool v) {
    ifu_valid_update(v);
}

extern "C" void perf_lsu_state_update(bool ren, bool wen, bool waiting, uint32_t addr) {
    lsu_state_update(ren, wen, waiting, addr);
}

extern "C" void perf_icache_valid_update(bool valid) {
    icache_valid_update(valid);
}

extern "C" void perf_icache_start_update(bool start) {
    icache_start_update(start);
}

extern "C" void perf_icache_is_hit_update(bool isHit) {
    icache_is_hit_update(isHit);
}

