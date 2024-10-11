#include "am.h"
#include "klib.h"
#include <stdint.h>

Context *handler(Event ev, Context *ctx) {
    if (ev.event == EVENT_ILLEGAL_INST) {
        printf("Illegal instruction at %p\n", ctx->mepc);
        ctx->gpr[10] = 0;
        (*(volatile uint32_t *)ctx->mepc) = 0x00100073;
    } else {
        printf("Unhandled event: %d\n", ev.event);
    }
    return ctx;
}

int main() {
    cte_init(handler);
    // yield();
    uint32_t buffer = 0;
    ((void (*)(void))&buffer)(); // illegal instruction
    return 0;
}
