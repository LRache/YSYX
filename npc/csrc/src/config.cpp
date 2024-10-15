#include <string>

#include "config.h"

bool config::hasNVBoard  = false;
bool config::hasDifftest = true;
bool config::perf = true;

bool config::statistic = false;
std::string config::statisticOutputFileName;

bool config::itrace = false;
std::string config::itraceOutputFileName;
bool config::ictrace = false;
std::string config::ictraceOutputFileName;
bool config::dtrace = false;
std::string config::dtraceOutputFileName;
bool config::zip = false;

bool config::loadRom = false;
std::string config::romImgFileName;
bool config::loadFlash = false;
std::string config::flashImgFileName;

bool config::allowIllegalInstruction = false;
