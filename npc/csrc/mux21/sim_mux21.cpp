#include "verilated.h"
#include "verilated_fst_c.h"
#include "obj/Vmux21.h"
#include "nvboard.h"
#include <unistd.h>

static VerilatedContext* contextp = NULL;
static VerilatedFstC* tfp = NULL;

static Vmux21* top;

void step_and_dump_wave(){
  top->eval();
  contextp->timeInc(1);
  tfp->dump(contextp->time());
  nvboard_update();
}

int main() {
  contextp = new VerilatedContext;
  tfp = new VerilatedFstC;
  top = new Vmux21;
  contextp->traceEverOn(true);
  top->trace(tfp, 0);
  tfp->open("trace.fst");

  nvboard_init();
  nvboard_bind_pin(&top->s, 1, SW0);
  nvboard_bind_pin(&top->a, 1, SW1);
  nvboard_bind_pin(&top->b, 1, SW2);
  nvboard_bind_pin(&top->y, 1, SW3);

  while (1)
  {
    for (int s = 0; s < 2; s++) {
      for (int a = 0; a < 2; a++) {
          for (int b = 0; b < 2; b++) {
              top->s = s;
              top->a = a;
              top->b = b;
              step_and_dump_wave();
          }
      }
    }
  }

  tfp->close();
  nvboard_quit();
  return 0;
}