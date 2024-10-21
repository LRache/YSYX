#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>

void __am_get_cur_as(Context *c);
void __am_switch(Context *c);

static Context* (*user_handler)(Event, Context*) = NULL;

Context* __am_irq_handle(Context *c) {
  // __am_get_cur_as(c);
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
      case IRQ_SYSCALL:
        ev.event = c->gpr[15] == -1 ? EVENT_YIELD : EVENT_SYSCALL; break;
      case IRQ_TIMER: 
        ev.event = EVENT_IRQ_TIMER; break;
      default: 
        assert(0);
    }
    c->mepc += 4;
    c = user_handler(ev, c);
    assert(c != NULL);
  }

  // __am_switch(c);
  return c;
}

extern void __am_asm_trap(void);

bool cte_init(Context*(*handler)(Event, Context*)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));
  // register event handler
  user_handler = handler;
  return true;
}

Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  Context *context = (Context *)(kstack.end - sizeof(Context) - sizeof(Context *));
  *(Context **)(kstack.end - sizeof(Context)) = context;
  context->mepc = (uintptr_t)entry;
  context->mstatus = 0x1800 | (1 << 3) | (1 << 7);
  context->gpr[10] = (uintptr_t)arg;
  context->gpr[2] = (uintptr_t)context;
  context->privilege = KERNEL;
  return context;
}

void set_mscratch(uint32_t value) {
  asm volatile ("csrw mscratch, %0" : : "r" (value));
}

void yield() {
#ifdef __riscv_e
  asm volatile("li a5, -1; ecall");
#else
  asm volatile("li a7, -1; ecall");
#endif
}

bool ienabled() {
  return false;
}

void iset(bool enable) {
}