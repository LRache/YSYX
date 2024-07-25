#include <common.h>
#include "syscall.h"
#include "fs.h"
#include "klib.h"
#include "proc.h"

struct timeval {
  long s;
  long us;
};

static int sys_exit  (int r);
static int sys_yield ();
static int sys_open  (char *pathname, int flags, int mode);
static int sys_read  (int fd, void *buf, size_t len);
static int sys_write (int fd, const void *buf, size_t count);
static int sys_close (int fd);
static int sys_lseek (int fd, off_t offset, int whence);
static int sys_brk   (uintptr_t brk);
static int sys_execve(char *pathname, char *const argv[], char *const envp[]);
static int sys_gettimeofday(struct timeval *t);
static int sys_ioe_read (int reg, void *buf);
static int sys_ioe_write(int reg, void *buf);

void do_syscall(Context *c) {
  uintptr_t s = c->GPR1;
  uintptr_t arg[3];
  arg[0] = c->GPR2;
  arg[1] = c->GPR3;
  arg[2] = c->GPR4;

  uintptr_t r = 0;
  switch (s) {
    case SYS_exit:  
      r = sys_exit((int)arg[0]); break;
    case SYS_yield: 
      r = sys_yield(); break;
    case SYS_open:
      r = sys_open((char *)arg[0], arg[1], arg[2]); break;
    case SYS_read:
      r = sys_read(arg[0], (void *)arg[1], arg[2]); break;
    case SYS_write: 
      r = sys_write(arg[0], (void *)arg[1], arg[2]); break;
    case SYS_close:
      r = sys_close(arg[0]); break;
    case SYS_lseek:
      r = sys_lseek(arg[0], arg[1], arg[2]); break;
    case SYS_brk:
      r = sys_brk(arg[0]); break;
    case SYS_execve:
      r = sys_execve((char *)arg[0], (char **)arg[1], (char **)arg[2]); break;
    case SYS_gettimeofday:
      r = sys_gettimeofday((struct timeval *)arg[0]); break;
    case SYS_ioe_read:
      sys_ioe_read(arg[0], (void *)arg[1]); break;
    case SYS_ioe_write:
      sys_ioe_write(arg[0], (void *)arg[1]); break;
    default: panic("Unhandled syscall ID = %d", s);
  }
  c->GPRx = r;
}

static int sys_exit(int r) {
  proc_exit(r);
  return 0;
}

static int sys_yield() {
  yield();
  return 0;
}

static int sys_open(char *pathname, int flags, int mode) {
  return fs_open(pathname, flags, mode);
}

static int sys_read(int fd, void *buf, size_t len) {
  return fs_read(fd, buf, len);
}

static int sys_write(int fd, const void *buf, size_t count) {
    return fs_write(fd, buf, count);
}

static int sys_close(int fd) {
  return fs_close(fd);
}

static int sys_lseek(int fd, off_t offset, int whence) {
  return fs_lseek(fd, offset, whence);
}

static int sys_brk(uintptr_t brk) {
  return mm_brk(brk);
}

static int sys_execve(char *pathname, char *const argv[], char *const envp[]) {
  execve(pathname, argv, envp);
  return 0;
}

static int sys_gettimeofday(struct timeval *t) {
  uint64_t us = io_read(AM_TIMER_UPTIME).us;
  t->s  = us / 1000000;
  t->us = us % 1000000;
  return 0;
}

static int sys_ioe_read(int reg, void *buf) {
  ioe_read(reg, buf);
  return 0;
}

static int sys_ioe_write(int reg, void *buf) {
  ioe_write(reg, buf);
  return 0;
}
