/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <readline/readline.h>
#include <readline/history.h>

#include "isa.h"
#include "sdb.h"
#include "memory/vaddr.h"
#include "memory/paddr.h"
#include "cpu/cpu.h"
#include "tracer.h"

static int is_batch_mode = false;

void init_regex();
void init_wp_pool();

/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(nemu) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_c(char *args) {
  cpu_exec(-1);
  return 0;
}


static int cmd_q(char *args) {
  return -1;
}

static int cmd_help(char *args);
static int cmd_si(char *args);
static int cmd_info(char *args);
static int cmd_x(char *args);
static int cmd_p(char *args);
static int cmd_w(char *args);
static int cmd_d(char *args);
static int cmd_trace(char *args);

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },
  { "si","Step execute", cmd_si },
  { "info", "Show info: r/reg w", cmd_info },
  { "x", "Read memory", cmd_x },
  { "p", "Show the value of the expr", cmd_p },
  { "w", "Set watchpoint", cmd_w },
  { "d", "Delete watchpoint", cmd_d },
  { "trace", "Trace instructions, memeory or function call.", cmd_trace }
};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

static int cmd_si(char *args) {
  int i;
  int n = sscanf(args, "%d", &i);
  if (n != 1) {
    printf("Invalid step: %s", args);
    return 1;
  }
  cpu_exec(i);
  return 0;
}

static int cmd_info(char *args) {
  if (args == NULL) {
    printf("info command needs args.\n");
    return 1;
  }
  char buffer[100];
  int n = sscanf(args, "%s", buffer);
  if (n == 0) {
    printf("Invalid args.\n");
    return 1;
  }
  if (strcmp(buffer, "reg") == 0 || strcmp(buffer, "r") == 0) {
    isa_reg_display();
    return 0;
  }
  if (strcmp(buffer, "w") == 0) {
    watchopint_display();
    return 0;
  }
  if (strcmp(buffer, "pc") == 0) {
    printf("pc=0x%x\n", cpu.pc);    
    return 0;
  }
  printf("Command not found: %s\n", buffer);
  return 1;
}

static int cmd_x(char* args) {
  int n;
  vaddr_t addr;
  int t = sscanf(args, "%d " FMT_PADDR, &n, &addr);
  if (t != 2) {
    printf("Invalid args: %s\n", args);
    return 1;
  }
  for (int i = 0; i < n; i++)
  {
    word_t word = vaddr_read(addr, 4);
    printf(FMT_PADDR ": 0x%08x\n", addr, word);
    addr += 4;
  }
  return 0;
}

static int cmd_p(char *args) {
  bool success;
  word_t result;
  result = expr(args, &success);
  if (success) {
    printf("%u\n", result);
    return 0;
  } else {
    printf("Invalid expression!\n");
    return 1;
  }
}

static int cmd_w(char *args) {
  bool success;
  word_t value = expr(args, &success);
  if (!success) {
    printf("Invalid expression!\n");
    return 1;
  }
  
  WP *wp = new_wp();
  strncpy(wp->expr, args, 31);
  wp->value = value;
  
  return 0;
}

static int cmd_trace(char *args) {
  if (strcmp(args, "i") == 0 || strcmp(args, "ins") == 0) {
    ins_trace_display();
    return 0;
  }
  if (strcmp(args, "m") == 0 || strcmp(args, "mem") == 0) {
    mem_trace_display();
    return 0;
  }
  if (strcmp(args, "f") == 0 || strcmp(args, "func") == 0) {
    function_trace_display();
  }
  if (strcmp(args, "d") == 0 || strcmp(args, "dev") == 0) {
    device_trace_display();
  }
  return 1;
}

static int cmd_d(char *args) {
  int n;
  sscanf(args, "%d", &n);
  int t = delete_wp(n);
  if (t) {
    printf("Cannot delete watchpoints %d\n", n);
    return 1;
  }
  return 0;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

bool watchpoint_triggered() {
  WP *node = wp_head();
  while (node) {
    bool s;
    if (expr(node->expr, &s) != node->value) {
      printf("Watchpoint %d triggered.\n", node->NO);
      return true;
    }
    node = node->next;
  }
  return false;
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}
