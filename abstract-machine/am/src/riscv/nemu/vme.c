#include <am.h>
#include <nemu.h>
#include <klib.h>

static AddrSpace kas = {};
static void* (*pgalloc_usr)(int) = NULL;
static void (*pgfree_usr)(void*) = NULL;
static int vme_enable = 0;

static Area segments[] = {      // Kernel memory mappings
  NEMU_PADDR_SPACE
};

#define USER_SPACE RANGE(0x40000000, 0x80000000)

static inline void set_satp(void *pdir) {
  uintptr_t mode = 1ul << (__riscv_xlen - 1);
  asm volatile("csrw satp, %0" : : "r"(mode | ((uintptr_t)pdir >> 12)));
}

static inline uintptr_t get_satp() {
  uintptr_t satp;
  asm volatile("csrr %0, satp" : "=r"(satp));
  return satp << 12;
}

bool vme_init(void* (*pgalloc_f)(int), void (*pgfree_f)(void*)) {
  pgalloc_usr = pgalloc_f;
  pgfree_usr = pgfree_f;

  kas.ptr = pgalloc_f(PGSIZE);
  kas.pgsize = PGSIZE;

  int i;
  for (i = 0; i < LENGTH(segments); i ++) {
    void *va = segments[i].start;
    for (; va < segments[i].end; va += PGSIZE) {
      map(&kas, va, va, 0);
    }
  }

  set_satp(kas.ptr);
  vme_enable = 1;

  return true;
}

void protect(AddrSpace *as) {
  PTE *updir = (PTE*)(pgalloc_usr(PGSIZE));
  as->ptr = updir;
  as->area = USER_SPACE;
  as->pgsize = PGSIZE;
  // map kernel space
  memcpy(updir, kas.ptr, PGSIZE);
}

void unprotect(AddrSpace *as) {
}

void __am_get_cur_as(Context *c) {
  c->pdir = (vme_enable ? (void *)get_satp() : NULL);
}

void __am_switch(Context *c) {
  if (vme_enable && c->pdir != NULL) {
    set_satp(c->pdir);
  }
}

void map(AddrSpace *as, void *va, void *pa, int prot) {
  if (!prot) pa = va;
  uint32_t vaddr = (uint32_t)va;
  uint32_t paddr = (uint32_t)pa;

  PTE *pt = (PTE *)as->ptr;
  uint32_t vpn[2];
  vpn[0] = (vaddr >> 12) & 0x3ff;
  vpn[1] = (vaddr >> 22) & 0x3ff;
  
  
  uint32_t v = pt[vpn[1]] & 0x1;
  uint32_t ppa;
  if (!v) {
    ppa = (uint32_t)pgalloc_usr(as->pgsize) & ~(as->pgsize - 1);
    pt[vpn[1]] = (ppa >> 2) | 0x1;
  } else {
    ppa = (pt[vpn[1]] & ~0x3ff) << 2;
  }
  pt = (uint32_t *)ppa;
  pt[vpn[0]] = ((paddr & ~(as->pgsize - 1)) >> 2) | 0xf;
}

Context *ucontext(AddrSpace *as, Area kstack, void *entry) {
  Context *context = (Context *)(kstack.end - sizeof(Context));
  context->mepc = (uintptr_t)entry - 4;
  context->mstatus = 0x1800 | (1 << 3) | (1 << 7);
  context->pdir = as->ptr;
  context->privilege = USER;
  context->gpr[2] = (uintptr_t)context;
  return context;
}
