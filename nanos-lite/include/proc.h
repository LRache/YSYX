#ifndef __PROC_H__
#define __PROC_H__

#include <common.h>
#include <memory.h>

#define STACK_SIZE (8 * PGSIZE)

typedef struct {
  uint8_t stack[STACK_SIZE] PG_ALIGN;
  Context *cp;
  AddrSpace as;
  uintptr_t max_brk;
} PCB;

extern PCB *current;

Context* schedule(Context *prev);
uintptr_t loader(PCB *pcb, const char *filename, AddrSpace *as);
void naive_uload(PCB *pcb, const char *filename);
void context_uload(PCB *pcb, const char *filename, char *const argv[], char *const envp[]);
void execve(const char *filename, char *const argv[], char *const envp[]);
void proc_exit(int r);

#endif
