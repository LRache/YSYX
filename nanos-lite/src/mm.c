#include <memory.h>
#include <proc.h>

static void *pf = NULL;\
extern PCB *pcb;

void* new_page(size_t nr_page) {
  void *p = pf;
  pf += nr_page * PGSIZE;
  return p;
}

#ifdef HAS_VME
void* pg_alloc(int n) {
  void *p = new_page(n / PGSIZE);
  memset(p, 0, n);
  return p;
}
#endif

void free_page(void *p) {
  panic("not implement yet");
}

/* The brk() system call handler. */
int mm_brk(uintptr_t brk) {
  if (current->max_brk == 0) {
    current->max_brk = brk;
    return 0;
  }
  if (brk < current->max_brk) return -1;
  uint32_t oldVpn = current->max_brk / PGSIZE;
  uint32_t newVpn = brk / PGSIZE;
  for (uint32_t i = oldVpn + 1; i <= newVpn; i++) {
    void *vaddr = (void *)(i * PGSIZE);
    void *paddr = pg_alloc(PGSIZE);
    map(&current->as, vaddr, paddr, 1);
  }
  current->max_brk = brk;
  return 0;
}

void init_mm() {
  pf = (void *)ROUNDUP(heap.start, PGSIZE);
  Log("free physical pages starting from %p", pf);

#ifdef HAS_VME
  vme_init(pg_alloc, free_page);
#endif
}
