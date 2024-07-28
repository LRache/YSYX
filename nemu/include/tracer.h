#ifndef __CPU_TRACER_H__
#define __CPU_TRACER_H__

#include "common.h"
#include "cpu/decode.h"

typedef struct SymTableEntry{
  vaddr_t start;
  vaddr_t end;
  char name[12];
  struct SymTableEntry *next;
} SymTableEntry;

enum CALL_TRACE_TYPE {
    FUN_CAL, FUN_RET
};

typedef struct CallLinkNode
{
    word_t pc;
    vaddr_t dst;
    struct CallLinkNode *next;
    int type; 
} FunTracer;

#define BUFFER_TRACER_HEADER int start, end;

#define INST_TRACER_SIZE 32

typedef struct {
  BUFFER_TRACER_HEADER
  char inst[INST_TRACER_SIZE][128];
} InstTracer;

#define DEVICE_TRACER_SIZE 32

enum {
  DEVICE_READ, DEVICE_WRITE
};

typedef struct {
  char name[32];
  paddr_t pc;
  paddr_t addr;
  int len;
  paddr_t offset;
  int type;
} DeviceTracerEntry;

typedef struct
{
  BUFFER_TRACER_HEADER;
  DeviceTracerEntry trace[DEVICE_TRACER_SIZE];
} DeviceTracer;


void add_sym_table_entry(vaddr_t addr, int size, char *name);
void free_sym_table();

void trace_function(Decode *_this);
void free_function_tracer();
void function_trace_display();

void trace_ins(Decode *_this);
void ins_trace_display();

void trace_device(const char *name, paddr_t addr, paddr_t offset, int type, paddr_t pc, int len);
void device_trace_display();

#endif