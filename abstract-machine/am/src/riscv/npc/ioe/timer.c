#include <am.h>
#include <stdint.h>

#define CLINT_BASE 0x02000000
#define CLINT_LOW  *(volatile uint32_t *)(CLINT_BASE + 0)
#define CLINT_HIGH *(volatile uint32_t *)(CLINT_BASE + 4)
#define FREQUENCY  700

void __am_timer_init() {
}

void __am_timer_config(AM_TIMER_CONFIG_T *cfg) { 
    cfg->present = true; cfg->has_rtc = true; 
}

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
    uint32_t low  = CLINT_LOW;
    uint32_t high = CLINT_HIGH;
    if (low == 0) {
        low  = CLINT_LOW;
        high = CLINT_HIGH;
    }
    uptime->us = (((uint64_t)high << 32) | low) / FREQUENCY;
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
    rtc->second = 0;
    rtc->minute = 0;
    rtc->hour   = 0;
    rtc->day    = 0;
    rtc->month  = 0;
    rtc->year   = 1900;
}
