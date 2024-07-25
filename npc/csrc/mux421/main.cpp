#include <iostream>
#include <verilated.h>
#include <verilated_fst_c.h>
#include "obj/Vmux421.h"
#include "nvboard.h"

Vmux421 *top = new Vmux421;
VerilatedContext *context = new VerilatedContext;
VerilatedFstC *fp = new VerilatedFstC;

void step_and_trace() {
    top->eval();
    context->timeInc(1);
    fp->dump(context->time());
}

void test_nvboard() {
    nvboard_init();
    nvboard_bind_pin(&top->y,  2, SW1, SW0);
    nvboard_bind_pin(&top->x0, 2, SW3, SW2);
    nvboard_bind_pin(&top->x1, 2, SW5, SW4);
    nvboard_bind_pin(&top->x2, 2, SW7, SW6);
    nvboard_bind_pin(&top->x3, 2, SW9, SW8);
    nvboard_bind_pin(&top->f,  2, LD1, LD0);
    while (1)
    {
        top->eval();
        nvboard_update();
    }
    nvboard_quit();
}

void test_trace() {
    context = new VerilatedContext;
    top = new Vmux421;
    fp = new VerilatedFstC;
    context->traceEverOn(true);
    top->trace(fp, 0);
    fp->open("./trace.fst");

    for (int y = 0; y < 4; y++) {
        top->y = y;
        for (int x0 = 0; x0 < 4; x0++) {
            top->x0 = x0;
            for (int x1 = 0; x1 < 4; x1++) {
                top->x1 = x1;
                for (int x2 = 0; x2 < 4; x2++) {
                    top->x2 = x2;
                    for (int x3 = 0; x3 < 4; x3++) {
                        top->x3 = x3;
                        step_and_trace();
                        int a;
                        switch (y)
                        {
                            case 0: a = x0; break;
                            case 1: a = x1; break;
                            case 2: a = x2; break;
                            case 3: a = x3; break;
                            default: break;
                        }
                        if (a != top->f) {
                            printf("y=%d, f=%d, x0=%d, x1=%d, x2=%d, x3=%d, a=%d\n", top->y, top->f, top->x0, top->x1, top->x2, top->x3, a);
                            assert(0);
                        }
                    }
                }
            }
        }
    }
    fp->close();
}

int main() {
    test_nvboard();
    return 0;
}
