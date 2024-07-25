#include <verilated.h>
#include <iostream>
#include "obj/Valu4.h"

#define CHECK(ans) top->in_s = s; top->eval();\
if (top->out_s != ((ans) & 0xf)) \
{printf("%d %d %d %d %d\n", (int8_t)top->in_x, (int8_t)top->in_y, (int8_t)top->in_s, (int8_t)top->out_s, (int8_t)(ans) & 0xf); assert(0);} \
s++;

Valu4 *top;

int main() {
    VerilatedContext *context = new VerilatedContext;
    top = new Valu4;

    for (int8_t a = -8; a < 8; a++) {
        int8_t x = a & 0xf;
        top->in_x = x;
        for (int8_t b = -8; b < 8; b++) {
            int8_t y = b & 0xf;
            top->in_y = y;
            uint8_t s = 0;
            CHECK(x + y);
            CHECK(x - y);
            CHECK(~x);
            CHECK(x & y);
            CHECK(x | y);
            CHECK(x ^ y);
            CHECK(a < b);
            CHECK(x == y);
        }
    }
    return 0;
}
