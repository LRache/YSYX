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

#include <isa.h>
#include "memory/vaddr.h"

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>

enum {
  TK_NOTYPE,
  TK_LEFT,
  TK_RIGHT,
  TK_EQ,
  TK_NE,
  TK_GT,
  TK_GE,
  TK_LT,
  TK_LE,
  TK_AND,
  TK_PLUS,
  TK_SUB,
  TK_MULTIPY,
  TK_DIVIDE,
  TK_NUMBER_DEC,
  TK_NUMBER_HEX_x,
  TK_NUMBER_HEX_X,
  TK_VAR,
  TK_DEREF
  /* TODO: Add more token types */

};

static const int DEREF_PREFIX_OP[] = {
  TK_LEFT,
  TK_EQ,
  TK_NE,
  TK_GT,
  TK_GE,
  TK_LT,
  TK_LE,
  TK_AND,
  TK_PLUS,
  TK_SUB,
  TK_MULTIPY,
  TK_DIVIDE,
};

#define NR_DEREF_PREFIX_OP ARRLEN(DEREF_PREFIX_OP)

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   */

  {" +",              TK_NOTYPE},       // spaces
  {"\\(",             TK_LEFT},         // left parentheses
  {"\\)",             TK_RIGHT},        // right parentheses
  {"==",              TK_EQ},           // equal
  {"!=",              TK_NE},           // not equal
  {">=",              TK_GE},           // greater or equal
  {">",               TK_GT},           // greater than
  {"<=",              TK_LE},           // less or equal
  {"<",               TK_LT},           // less than
  {"\\&\\&",          TK_AND},          // and
  {"\\+",             TK_PLUS},         // plus
  {"\\-",             TK_SUB},          // sub
  {"\\*",             TK_MULTIPY},      // multipy
  {"\\/",             TK_DIVIDE},       // divide
  {"0x[0-9a-fA-F]+",  TK_NUMBER_HEX_x}, // hex number 0x    
  {"0X[0-9a-fA-F]+",  TK_NUMBER_HEX_X}, // hex number 0X
  {"[0-9]+",          TK_NUMBER_DEC},   // dec number
  {"^\\$([a-zA-Z0-9$]+)", TK_VAR}        // variable
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

static Token tokens[32] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        // Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
        //     i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */

        // switch (rules[i].token_type) {
        //   case 
        //   default: TODO();
        // }
        if (nr_token == 31) {
          Log("Expression is too long!");
          return false;
        }
        if (rules[i].token_type != TK_NOTYPE) {
          Token token;
          token.type = rules[i].token_type;
          for(int i = 0; i < 32 && i < substr_len; i++) {
            token.str[i] = *(substr_start+i);
          }
          token.str[substr_len] = '\0';
          tokens[nr_token++] = token;
        }
        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  if (tokens[0].type == TK_MULTIPY) {
    tokens[0].type = TK_DEREF;
  }
  for (int i = 1; i < nr_token; i++)
  {
    if (tokens[i].type == TK_MULTIPY) {
      for (int j = 0; j < NR_DEREF_PREFIX_OP; j++)
      {
        if (tokens[i-1].type == DEREF_PREFIX_OP[j]) {
          tokens[i].type = TK_DEREF;
          break;
        }
      }
    }
  }
  

  return true;
}

bool packed_by_parentheses(int start, int end) {
  if (tokens[start].type != TK_LEFT || tokens[end-1].type != TK_RIGHT) {
    return false;
  }
  int counter = 0;
  for (int i = start; i < end; i++) {
    if (tokens[i].type == TK_LEFT) {
      counter++;
    }
    else if (tokens[i].type == TK_RIGHT) {
      counter--;
      if (counter == 0) {
        if (i == end-1) {
          return true;
        } else {
          return false;
        }
      }
    }
  }
  Log("Invalid return: start=%d, end=%d", start, end);
  return false;
}

word_t eval(bool *success, int start, int end) {
  if (!*success) return 0;
  if (start == end) {
    *success = false;
    return 0;
  }
  if (start+1 == end) {
    word_t num = 0;
    if (tokens[start].type == TK_NUMBER_DEC) {
      sscanf(tokens[start].str, "%u", &num);
    } 
    else if (tokens[start].type == TK_NUMBER_HEX_x) {
      sscanf(tokens[start].str, "0x%x", &num);
    } 
    else if (tokens[start].type == TK_NUMBER_HEX_X) {
      sscanf(tokens[start].str, "0X%x", &num);
    } 
    else if (tokens[start].type == TK_VAR) {
      num = isa_reg_str2val(tokens[start].str+1, success);
    }
    else {
      *success = false;
    }
    return num;
  }
  
  if (packed_by_parentheses(start, end)) {
    return eval(success, start+1, end-1);
  }
  
  int op = -1;
  int priority = 0;
  for (int i = start; i < end; i++) {
    int t = tokens[i].type;
    if (t == TK_LEFT) {
      int counter = 1;
      for (i++; i < end; i++) {
        if (tokens[i].type == TK_LEFT) counter++;
        else if (tokens[i].type == TK_RIGHT) {
          counter--;
          if (counter == 0) break;
        }
      }
      continue;
    }
    else if (t == TK_EQ || t == TK_NE || t == TK_GT || t == TK_GE) {
      op = i;
      break;
    }
    else if (t == TK_PLUS || t == TK_SUB) {
      priority = 2;
      op = i;
    }
    else if (priority < 1 && (t == TK_MULTIPY || t == TK_DIVIDE)) {
      op = i;
      priority = 1;
    }
    else if (priority == 0 && t == TK_DEREF) {
      op = i;
    }
  }
  if (op == -1) {
    *success = false;
    return 0;
  }

  if (tokens[op].type == TK_DEREF) {
    word_t rightValue = eval(success, op+1, end);
    word_t result = vaddr_read(rightValue, sizeof(word_t));
    return result;
  }

  word_t leftValue = eval(success, start, op);
  word_t rightValue = eval(success, op+1, end);
  if (!*success) return 0;
  
  switch (tokens[op].type)
  {
  case TK_EQ:
    return leftValue == rightValue;

  case TK_NE:
    return leftValue != rightValue;

  case TK_GE:
    return leftValue >= rightValue;

  case TK_GT:
    return leftValue > rightValue;

  case TK_LE:
    return leftValue <= rightValue;

  case TK_LT:
    return leftValue < rightValue;
  
  case TK_PLUS:
    return leftValue + rightValue;
  
  case TK_SUB:
    return leftValue - rightValue;
  
  case TK_MULTIPY:
    return leftValue * rightValue;
  
  case TK_DIVIDE:
    if (rightValue == 0) {
      printf("Divided by zero error\n");
      *success = false;
      return 0;
    }
    return leftValue / rightValue;
  
  default:
    break;
  }
  
  return 0;
}

bool matched_parentheses(int start, int end) {
  int counter = 0;
  for (int i = start; i < end; i++) {
    if (tokens[i].type == TK_LEFT) {
      counter++;
    }
    else if (tokens[i].type == TK_RIGHT) {
      counter--;
      if (counter < 0) {
        return false;
      }
    }
  }
  return counter == 0;
}

word_t expr(char *e, bool *success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  /* TODO: Insert codes to evaluate the expression. */
  if (!matched_parentheses(0, nr_token)) {
    *success = false;
    printf("Bad parentheses!\n");
    return 0;
  }
  *success = true;
  word_t result = eval(success, 0, nr_token);

  return result;
}
