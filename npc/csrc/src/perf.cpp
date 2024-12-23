#include <iostream>
#include <iomanip>
#include <ostream>

#include "hdb.h"
#include "debug.h"
#include "perf.h"
#include "memory.h"
#include "config.h"

#ifdef PERF

#define CHECK if (!config::perf) return ;

#define STATISTIC_OUTPUT_INIT stream << std::fixed << std::setprecision(2);
#define STATISTIC_OUTPUT_DEINIT stream.unsetf(std::ios::fixed); stream.unsetf(std::ios::floatfield);

struct Counter {
    uint64_t count;
    uint64_t clockCount;
    double average() const {
        if (count == 0) return 0;
        return (double)clockCount / count;
    }

    void pref_count(uint64_t c) {
        if (!cpu.running) return ;
        count ++;
        clockCount += c;
    }
};

static struct {
    Counter hit;
    Counter miss;

    uint64_t start;
    bool isStart = false;
    bool iduReady = false;

    word_t pc;
    Counter flash;
    Counter sdram;
    Counter other;
} icache;

void perf::idu_ready_update(bool ready) {
    CHECK;
    icache.iduReady = ready;
}

void perf::icache_mem_valid_update(bool valid) {
    CHECK;
    if (valid) {
        uint64_t clockCount = cpu.clockCount - icache.start;
        icache.miss.pref_count(clockCount);
        if (in_flash(icache.pc)) icache.flash.pref_count(clockCount);
        else if (in_sdram(icache.pc)) icache.sdram.pref_count(clockCount);
        else icache.other.pref_count(clockCount);
    }
}

void perf::icache_mem_start_update(bool start) {
    CHECK;
    if ((!icache.isStart) && start) {
        icache.start = cpu.clockCount;
    }
    icache.isStart = start;
}

void perf::icache_is_hit_update(bool isHit) {
    CHECK;
    if (isHit && icache.iduReady) {
        icache.hit.pref_count(0);
    }
}

void perf::icache_pc_update(word_t pc) {
    CHECK;
    icache.pc = pc;
}

static inline void print_icache_statistic(std::ostream &stream, const std::string &name, const Counter &c) {
    stream
    << std::setw( 6) << name << " | "
    << std::setw(12) << c.clockCount << " | " 
    << std::setw(10) << c.count << " | " 
    << std::setw( 8) << c.average() << " | "
    << std::setw( 5) << (double)c.clockCount / cpu.clockCount * 100 << "%" << std::endl;
}

void perf::icache_statistic(std::ostream &stream) {
    CHECK;
    stream << "Performance Statistic of ICache" << std::endl;
    stream << std::setw(5) << "" << " | " 
    << std::setw(10) << "Count" << " | " 
    << std::setw( 7) << " " << " | " 
    << std::endl;
    
    uint64_t total = icache.hit.count + icache.miss.count;
    uint64_t totalClk = icache.hit.clockCount + icache.miss.clockCount;
    STATISTIC_OUTPUT_INIT;
    stream 
    << std::setw( 5) << "hit" << " | "
    << std::setw(10) << icache.hit.count << " | "
    << std::setw( 6) << (double)icache.hit.count / total * 100 << "% | " 
    << std::endl;

    stream
    << std::setw( 5) << "miss" << " | "
    << std::setw(10) << icache.miss.count << " | "
    << std::setw( 6) << (double)icache.miss.count / total * 100 << "% | "
    << std::endl;

    stream 
    << std::setw( 5) << "Total" << " | "
    << std::setw(10) << total << " | " 
    << std::setw( 7) << "" << " | "
    << std::setw(12) << totalClk << " | "
    << std::endl;

    stream << std::endl;

    stream
    << std::setw( 6) << "Source" << " | "
    << std::setw(12) << "Clock" << " | " 
    << std::setw(10) << "Count" << " | " 
    << std::setw( 8) << "Average" << " | " << std::endl;

    print_icache_statistic(stream, "flash", icache.flash);
    print_icache_statistic(stream, "sdram", icache.sdram);
    print_icache_statistic(stream, "other", icache.other);

    STATISTIC_OUTPUT_DEINIT;

    double amat = totalClk + (1 - (double) icache.miss.clockCount / totalClk) * icache.miss.clockCount;
    stream << "AMAT=" << std::fixed << amat << std::endl;
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

void perf::lsu_state_update(bool ren, bool wen, bool waiting, addr_t addr) {
    CHECK;
    if (waiting) {
        lsu.start = cpu.clockCount;
        lsu.isWaiting = true;
        lsu.wen = wen;
        lsu.ren = ren;
        lsu.addr = addr;
    } else if (lsu.isWaiting) {
        lsu.isWaiting = false;
        uint64_t clockCount = cpu.clockCount - lsu.start;
        // if (clockCount > 5000) {
        //     if (lsu.ren) lsu.unexpRead.pref_count(clockCount);
        //     else if (lsu.wen) lsu.unexpWrite.pref_count(clockCount);
        // } else 
        if (in_flash(lsu.addr)) {
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

static void print_lsu_statistic(std::ostream &stream, const char *name, const Counter &c, bool is_read) {
    stream
    << std::setw( 6) << name << " | " 
    << (is_read ? " read" : "write") << " | "
    << std::setw(12) << c.clockCount << " | " 
    << std::setw(10) << c.count << " | " 
    << std::setw( 8) << c.average() << " | " 
    << std::setw( 5) << (double)c.clockCount / cpu.clockCount * 100 << "%" << std::endl;
}

void perf::lsu_statistic(std::ostream &stream) {
    CHECK;
    stream << "Performance Statistic of LSU" << std::endl;
    stream
    << std::setw( 6) << "Source" << " | "
    << " Type" << " | "
    << std::setw(12) << "Clock" << " | " 
    << std::setw(10) << "Count" << " | " 
    << std::setw( 8) << "Average" << std::endl;

    STATISTIC_OUTPUT_INIT;
    print_lsu_statistic(stream, "flash", lsu.flashRead, true);
    print_lsu_statistic(stream, "sram", lsu.sramRead, true);
    print_lsu_statistic(stream, "sram", lsu.sramWrite, false);
    print_lsu_statistic(stream, "sdram", lsu.sdramRead, true);
    print_lsu_statistic(stream, "sdram", lsu.sdramWrite, false);
    print_lsu_statistic(stream, "other", lsu.otherRead, true);
    print_lsu_statistic(stream, "other", lsu.otherWrite, false);
    print_lsu_statistic(stream, "unexp", lsu.unexpRead, true);
    print_lsu_statistic(stream, "unexp", lsu.unexpWrite, false);
    STATISTIC_OUTPUT_DEINIT;
}

struct BranchPredict
{
    uint64_t success = 0;
    uint64_t fail = 0;
    bool exuValid = false;
} branchPredict;

void perf::exu_valid_update(bool valid) {
    CHECK;
    branchPredict.exuValid = valid;
}

void perf::branch_predict_failed_update(bool failed) {
    CHECK;
    if (!cpu.running) return ;
    if (failed) branchPredict.fail++;
}

void perf::branch_predict_success_update(bool success) {
    CHECK;
    if (!cpu.running) return ;
    if (success && branchPredict.exuValid) {
        branchPredict.success++;
    }
}

void perf::branch_predictor_statistic(std::ostream &stream) {
    CHECK;
    stream << "Performance Statistic of Branch Predictor" << std::endl;
    stream 
    << std::setw( 7) << " " << " | "
    << std::setw(10) << "Count" << " | "
    << std::setw( 8) << "Rate" << " | " << std::endl;

    branchPredict.success --;
    STATISTIC_OUTPUT_INIT
    double successRate = (double)branchPredict.success / (branchPredict.success + branchPredict.fail);
    stream
    << std::setw( 7) << "success" << " | "
    << std::setw(10) << branchPredict.success << " | "
    << std::setw(8) << successRate * 100 << "%" << std::endl;
    stream
    << std::setw( 7) << "fail" << " | "
    << std::setw(10) << branchPredict.fail << " | "
    << std::setw( 8) << (1 - successRate) * 100 << "%" << std::endl;
    STATISTIC_OUTPUT_DEINIT
}

void perf::init() {
    CHECK;
    icache.start = cpu.clockCount;
    lsu.start = cpu.clockCount;
}

void perf::statistic(std::ostream &stream) {
    CHECK;
    icache_statistic(stream);
    stream << std::endl;
    lsu_statistic(stream);
    stream << std::endl;
    branch_predictor_statistic(stream);
}

#else

void perf::init() {}
void perf::statistic() {}
void perf::lsu_statistic() {}
void perf::icache_statistic() {}
void perf::lsu_state_update(bool ren, bool wen, bool waiting, addr_t addr) {}
void perf::icache_mem_valid_update(bool valid) {}
void perf::icache_mem_start_update(bool start) {}
void perf::icache_is_hit_update(bool isHit) {}
void perf::icache_pc_update(word_t pc) {}

extern "C" void perf_lsu_state_update(bool ren, bool wen, bool waiting, uint32_t addr) {}
extern "C" void perf_icache_valid_update(bool valid) {}
extern "C" void perf_icache_start_update(bool start) {}
extern "C" void perf_icache_is_hit_update(bool isHit) {}

#endif

