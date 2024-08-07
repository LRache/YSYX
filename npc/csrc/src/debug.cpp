#include <stdio.h>
#include <locale.h>
#include "debug.h"

FILE *log_fp;

static bool logEnable = false;

void init_log(bool on=true) {
    setlocale(LC_NUMERIC, "en_US");
    logEnable = on;
    if (on) log_fp = stdout;
    else log_fp = NULL;
}

bool log_enable() {
    return logEnable;
}
