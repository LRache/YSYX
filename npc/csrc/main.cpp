#include <stdio.h>
#include "verilated.h"
#include "../obj_dir/Vexample.h"

int main() {
  Vexample *example = new Vexample;
  for (int a = 0; a < 2; a++)
  {
    for (int b = 0; b < 2; b++) {
      example->a = a;
      example->b = b;
      example->eval();
      printf("a=%d, b=%d, f=%d, correct=%d\n", a, b, example->f, a^b);
    }
  }
  return 0;
}
