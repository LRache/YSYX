#include <iostream>
#include <verilated.h>
#include <verilated_fst_c.h>
#include "obj/Vencoder.h"
#include "nvboard.h"

Vencoder *top = NULL;
VerilatedContext *context = NULL;
VerilatedFstC *fp = NULL;

void step_and_trace() {
    top->eval();
    context->timeInc(1);
    fp->dump(context->time());
}

void test_nvboard() {
    nvboard_init();
    nvboard_bind_pin(&top->x,  8, SW7, SW6, SW5, SW4, SW3, SW2, SW1, SW0);
    nvboard_bind_pin(&top->en, 1, SW8);
    nvboard_bind_pin(&top->y,  3, LD2, LD1, LD0);
    while (1)
    {
        top->eval();
        nvboard_update();
    }
    nvboard_quit();
}

void test_trace() {
    context = new VerilatedContext;
    fp = new VerilatedFstC;
    context->traceEverOn(true);
    top->trace(fp, 0);
    fp->open("./trace.fst");
    fp->close();
}

int main() {
    top = new Vencoder;
    test_nvboard();
    return 0;
}
