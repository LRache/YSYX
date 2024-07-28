#include <stdint.h>
#include <stdlib.h>
#include <assert.h>

void __libc_init_array();
int main(int argc, char *argv[], char *envp[]);
extern char **environ;
void call_main(uintptr_t *args) {
  __libc_init_array();
  
  char *empty[] =  {NULL };
  environ = empty;
  int argc = *(int *)args;
  char **argv = (char **)(args + 1);
  char **envp = (char **)(args + argc + 2);
  int r = main(argc, argv, envp);
  
  exit(r);
  assert(0);
}
