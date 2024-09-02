#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <rtthread.h>

static Context* ev_handler(Event e, Context *c) {
  rt_thread_t pcb = rt_thread_self();
  rt_ubase_t *arg = (rt_ubase_t *)pcb->user_data;
  Context **to = (Context **)arg[0];
  Context **from = (Context **)arg[1];
  switch (e.event) {
    case EVENT_YIELD:
      if (from) { *from = c; }
      return *to;
    case EVENT_IRQ_TIMER: break;
    default: printf("Unhandled event ID = %d\n", e.event); assert(0);
  }
  return c;
}

void __am_cte_init() {
  cte_init(ev_handler);
}

void rt_hw_context_switch_to(rt_ubase_t to) {
  rt_thread_t pcb = rt_thread_self();
  rt_ubase_t saved = pcb->user_data;
  rt_ubase_t arg[2] = {to, 0};
  pcb->user_data = (rt_ubase_t)&arg;
  yield();
  pcb->user_data = saved;
}

void rt_hw_context_switch(rt_ubase_t from, rt_ubase_t to) {
  rt_thread_t pcb = rt_thread_self();
  rt_ubase_t saved = pcb->user_data;
  rt_ubase_t arg[2] = {to, from};
  pcb->user_data = (rt_ubase_t)&arg;
  yield();
  pcb->user_data = saved;
}

void rt_hw_context_switch_interrupt(void *context, rt_ubase_t from, rt_ubase_t to, struct rt_thread *to_thread) {
  assert(0);
}

void wrap(void *args) {
  void (*tentry)(void *) = ((void **)args)[0];
  void *p = ((void **)args)[1];
  void (*texit)() = ((void **)args)[2];
  tentry(p);
  texit();
}

rt_uint8_t *rt_hw_stack_init(void *tentry, void *parameter, rt_uint8_t *stack_addr, void *texit) {
  void **args = (void **)(ROUNDDOWN(stack_addr, sizeof(uintptr_t)) - 3 * sizeof(void *));
  args[0] = tentry;
  args[1] = parameter;
  args[2] = texit;
  void *stack_end = args;
  return (rt_uint8_t *)kcontext((Area){.start=NULL, .end=stack_end}, wrap, args);
}
