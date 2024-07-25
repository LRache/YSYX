/**
 *                             _ooOoo_
 *                            o8888888o
 *                            88" . "88
 *                            (| -_- |)
 *                            O\  =  /O
 *                         ____/`---'\____
 *                       .'  \\|     |//  `.
 *                      /  \\|||  :  |||//  \
 *                     /  _||||| -:- |||||-  \
 *                     |   | \\\  -  /// |   |
 *                     | \_|  ''\---/''  |   |
 *                     \  .-\__  `-`  ___/-. /
 *                   ___`. .'  /--.--\  `. . __
 *                ."" '<  `.___\_<|>_/___.'  >'"".
 *               | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *               \  \ `-.   \_ __\ /__ _/   .-` /  /
 *          ======`-.____`-.___\_____/___.-`____.-'======
 *                             `=---='
 *          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *                     佛祖保佑        永无BUG
 *            佛曰:
 *                   写字楼里写字间，写字间里程序员；
 *                   程序人员写程序，又拿程序换酒钱。
 *                   酒醒只在网上坐，酒醉还来网下眠；
 *                   酒醉酒醒日复日，网上网下年复年。
 *                   但愿老死电脑间，不愿鞠躬老板前；
 *                   奔驰宝马贵者趣，公交自行程序员。
 *                   别人笑我忒疯癫，我笑自己命太贱；
 *                   不见满街漂亮妹，哪个归得程序员？
*/
#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>

void __am_get_cur_as(Context *c);
void __am_switch(Context *c);

static Context* (*user_handler)(Event, Context*) = NULL;

Context* __am_irq_handle(Context *c) {
  // printf("__am_irq_handle ENTER, c.mcause=0x%x\n", c->mcause);
  __am_get_cur_as(c);
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
      case IRQ_YIELD: 
        ev.event = EVENT_YIELD;     break;
      case IRQ_TIMER: 
        ev.event = EVENT_IRQ_TIMER; break;
      default: 
        ev.event = EVENT_SYSCALL;   break;
    }
    // printf("ev.event=%u\n", ev.event);
    // printf("current.usp=0x%x\n", c->usp);
    // printf("current.pri=%d\n", c->privilege);
    // printf("current=%p\n", c);
    // printf("\ncall user_handler\n\n");
    c = user_handler(ev, c);
    // printf("current=%p\n", c);
    // printf("current.usp=0x%x\n", c->usp);
    // printf("current.pri=%d\n", c->privilege);
    // printf("RET\n\n");
    assert(c != NULL);
  }

  __am_switch(c);
  return c;
}

uint32_t ksp = 0;
extern void __am_asm_trap(void);

bool cte_init(Context*(*handler)(Event, Context*)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));
  // register event handler
  user_handler = handler;
  return true;
}

Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  Context *context = (Context *)(kstack.end - sizeof(Context));
  context->mepc = (uintptr_t)entry - 4;
  context->mstatus = 0x1800 | (1 << 3) | (1 << 7);
  context->gpr[10] = (uintptr_t)arg;
  context->gpr[2] = (uintptr_t)kstack.end;
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
