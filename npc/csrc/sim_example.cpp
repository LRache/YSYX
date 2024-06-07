#include <stdio.h>
#include <verilated.h>
#include <verilated_fst_c.h>
#include "./sim_example/Vexample.h"

int main(int argc, char**argv) {
  Verilated::commandArgs(argc, argv);
  Vexample *example = new Vexample;
  VerilatedFstC *tracer = new VerilatedFstC;
  Verilated::traceEverOn(true);
  example->trace(tracer, 99);
  vluint64_t timestamp = 0;
  tracer->open("./wave.fst");
  for (int a = 0; a < 2; a++)
  {
    for (int b = 0; b < 2; b++) {
      example->a = a;
      example->b = b;
      example->eval();
      printf("a=%d, b=%d, f=%d, correct=%d\n", a, b, example->f, a^b);
      tracer->dump(timestamp++);
    }
  }
  tracer->close();
  delete tracer;
  delete example;
  printf("finished.\n");
  return 0;
}
