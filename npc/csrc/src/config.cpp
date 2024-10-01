#include <string>

#include "config.h"

bool config::hasNVBoard  = false;
bool config::hasDifftest = true;
bool config::perf = true;

bool config::itrace = false;
std::string config::itraceOutputFileName;
bool config::ictrace = false;
std::string config::ictraceOutputFileName;

bool config::loadRom = false;
std::string config::romImgFileName;
bool config::loadFlash = false;
std::string config::flashImgFileName;
