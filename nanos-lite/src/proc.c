#include <proc.h>
#include <memory.h>
#include <am.h>

#define MAX_NR_PROC 4

static PCB pcb[MAX_NR_PROC] __attribute__((used)) = {};
static PCB *running[MAX_NR_PROC];
static int runningCount = 0;
static int runningIndex = 0;
static PCB pcb_boot = {};
PCB *current = NULL;

void switch_boot_pcb() {
  current = &pcb_boot;
}

void context_kload(PCB *p, void (*entry)(void *), void *arg) {
  Area kstack = {.start = p->stack, .end = p->stack+STACK_SIZE};
  Context *context = kcontext(kstack, entry, arg);
  p->cp = context;
}

void context_uload(PCB *p, const char *filename, char *const argv[], char *const envp[]) {
  AddrSpace as;
  protect(&as);
  uintptr_t entry = loader(p, filename, &as);
  p->cp = ucontext(&as, (Area){.start=p->stack, .end=p->stack+STACK_SIZE}, (void *)entry);
  p->as = as;

  for (int i = 1; i < 8; i++) {
    void *paddr = pg_alloc(PGSIZE);
    map(&as, (void *)(as.area.start - i * PGSIZE), paddr, 1);
  }
  
  char *top = (char *)pg_alloc(PGSIZE);
  map(&as, (void *)(as.area.end - PGSIZE), (void *)top, 1);
  top += PGSIZE;
  char *pgEnd = top;

  int argc = 0, envpc = 0;  
  while (argv[argc] != NULL) argc++;
  while (envp[envpc] != NULL) envpc++;
  char *argvPointer[argc], *envpPointer[envpc];
  for (int i = envpc - 1; i >= 0; i--) {
    int length = strlen(envp[i]) + 1;
    top -= length;
    memcpy(top, envp[i], length);
    envpPointer[i] = top;
  }
  top -= 4;
  *(uint32_t *)top = 0;
  for (int i = argc - 1; i >= 0; i--) {
    int length = strlen(argv[i]) + 1;
    top -= length;
    memcpy(top, argv[i], length);
    argvPointer[i] = top;
  }
  top -= 4;
  *(uint32_t *)top = 0;
  for (int i = envpc-1; i >= 0; i--) {
    top -= 4;
    *(char **)top = envpPointer[i];
  }
  top -= 4;
  *(uint32_t *)top = 0;
  for (int i = argc-1; i >= 0; i--) {
    top -= 4;
    *(char **)top = argvPointer[i];
  }
  top -= 4;
  *(uint32_t *)top = argc;
  p->cp->gpr[10] = (uintptr_t)(as.area.end) - (uintptr_t)(pgEnd - top);

  Log("pcb[%d].stack=%p", p-pcb, p->stack);
}

void execve(const char *filename, char *const argv[], char *const envp[]) {
  context_uload(current, filename, argv, envp);
  switch_boot_pcb();
  yield();
}

void hello_fun(void *arg) {
  int j = 1;
  while (1) {
    Log("Hello World from Nanos-lite with arg '%p' for the %dth time!", (uintptr_t)arg, j);
    j ++;
    yield();
  }
}

void init_proc() {
  switch_boot_pcb();

  Log("Initializing processes...");

  // load program here
  char *const argv[] = {"/bin/hplayer", "/home/rache/Music/xitiejie44100.pcm", "44100", NULL};
  char *const empty[] = {NULL};
  context_uload(&pcb[0], argv[0], argv, empty);
  context_uload(&pcb[1], "/bin/hello", empty, empty);
  running[0] = &pcb[0];
  running[1] = &pcb[1];
  runningCount = 2;
}

void proc_exit(int r) {
  Log("Exit with code %d, pid=%d", r, runningIndex);
  for (int i = runningIndex; i < runningCount-1; i++) {
    running[i] = running[i+1];
  }
  running[runningCount - 1] = NULL;
  runningCount--;
  if (runningCount == 0) halt(0);
  yield();
}

void set_mscratch(uint32_t value);

Context* schedule(Context *prev) {
  if (runningCount == 0) return prev;
  current->cp = prev;
  if (current == &pcb_boot) {
    runningIndex = 0;
    current = running[0];
  } else {
    runningIndex = (runningIndex + 1) % runningCount;
    current = running[runningIndex];
  }
  return current->cp;
}
