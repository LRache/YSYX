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

#ifndef __SDB_H__
#define __SDB_H__

#ifndef TARGET_SHARE

#include <common.h>

typedef struct watchpoint {
  int NO;
  struct watchpoint *next;

  char expr[32];
  word_t value;

} WP;

word_t expr(char *e, bool *success);
WP* new_wp();
WP* wp_head();
int delete_wp(int n);
int free_wp(WP *wp);
void watchopint_display();

#endif

#endif
