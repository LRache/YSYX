#include "sim_mux21/Vmux21.h"
#include <verilated.h>
#include <verilated_fst_c.h>

static VerilatedContext *context = NULL;
static VerilatedFstC *fp = NULL;

static Vmux21 *top = NULL;

void step_and_trace() {
    top->eval();
    context->timeInc(1);
    fp->dump(context->time());
}

int main() {
    top = new Vmux21;
    context = new VerilatedContext;
    fp = new VerilatedFstC;
    fp->open("./trace.fst");
    context->traceEverOn(true);
}