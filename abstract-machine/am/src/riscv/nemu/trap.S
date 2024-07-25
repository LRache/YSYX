#define concat_temp(x, y) x ## y
#define concat(x, y) concat_temp(x, y)
#define MAP(c, f) c(f)

#if __riscv_xlen == 32
#define LOAD  lw
#define STORE sw
#define XLEN  4
#else
#define LOAD  ld
#define STORE sd
#define XLEN  8
#endif

#define REGS_LO16(f) \
      f( 1)             f( 4) f( 5) f( 6) f( 7) f( 8) f( 9) \
f(10) f(11) f(12) f(13) f(14) f(15)
#ifndef __riscv_e
#define REGS_HI16(f) \
                                    f(16) f(17) f(18) f(19) \
f(20) f(21) f(22) f(23) f(24) f(25) f(26) f(27) f(28) f(29) \
f(30) f(31)
#define NR_REGS 32
#else
#define REGS_HI16(f)
#define NR_REGS 16
#endif

#define REGS(f) REGS_LO16(f) REGS_HI16(f)

#define PUSH(n) STORE concat(x, n), (n * XLEN)(sp);
#define POP(n)  LOAD  concat(x, n), (n * XLEN)(sp);

#define CONTEXT_SIZE  ((NR_REGS + 7) * XLEN)
#define OFFSET_CAUSE  ((NR_REGS + 0) * XLEN)
#define OFFSET_STATUS ((NR_REGS + 1) * XLEN)
#define OFFSET_EPC    ((NR_REGS + 2) * XLEN)
#define OFFSET_USP    ((NR_REGS + 4) * XLEN)
#define OFFSET_PRI    ((NR_REGS + 5) * XLEN)

#define USER    0
#define KERNEL  1

.align 3
.globl __am_asm_trap
.section .text
__am_asm_trap:
  csrrw sp, mscratch, sp
  bne sp, x0, save_context_user 
  csrr sp, mscratch

save_context_kernel:
  addi sp, sp, -CONTEXT_SIZE

  MAP(REGS, PUSH)
  PUSH(2)

  csrr t0, mcause
  csrr t1, mstatus
  csrr t2, mepc
  li t3, KERNEL

  STORE t0, OFFSET_CAUSE(sp)
  STORE t1, OFFSET_STATUS(sp)
  STORE t2, OFFSET_EPC(sp)
  STORE t3, OFFSET_PRI(sp)

  j irq_handle

save_context_user:
  addi sp, sp, -CONTEXT_SIZE

  MAP(REGS, PUSH)
  PUSH(2)

  csrr t0, mcause
  csrr t1, mstatus
  csrr t2, mepc
  csrr t3, mscratch
  li t4, USER

  STORE t0, OFFSET_CAUSE(sp)
  STORE t1, OFFSET_STATUS(sp)
  STORE t2, OFFSET_EPC(sp)
  STORE t3, OFFSET_USP(sp)
  STORE t4, OFFSET_PRI(sp)

  j irq_handle

irq_handle:
  # set mstatus.MPRV to pass difftest
  li a0, (1 << 17)
  or t1, t1, a0
  csrw mstatus, t1

  # support re-entry
  mv t4, zero
  csrw mscratch, t4

  mv a0, sp
  jal __am_irq_handle
  mv sp, a0

  LOAD t0, OFFSET_PRI(sp)
  li t1, KERNEL
  beq t0, t1, load_context_kernel

load_context_user:
  LOAD t1, OFFSET_STATUS(sp)
  LOAD t2, OFFSET_EPC(sp)
  csrw mstatus, t1
  csrw mepc, t2

  MAP(REGS, POP)
  POP(2)
  addi sp, sp, CONTEXT_SIZE
  csrw mscratch, sp
  LOAD sp, (OFFSET_USP-CONTEXT_SIZE)(sp)
  
  j return_from_trap

load_context_kernel:
  LOAD t1, OFFSET_STATUS(sp)
  LOAD t2, OFFSET_EPC(sp)
  csrw mstatus, t1
  csrw mepc, t2

  MAP(REGS, POP)
  POP(2)
  addi sp, sp, CONTEXT_SIZE

return_from_trap:
  mret
