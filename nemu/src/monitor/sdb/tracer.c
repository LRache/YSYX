#include "cpu/cpu.h"
#include "cpu/decode.h"
#include "tracer.h"

static SymTableEntry *symTable = NULL;
static SymTableEntry *symTableTail = NULL;

void add_sym_table_entry(vaddr_t addr, int size, char *name) {
    if (symTable == NULL) {
        symTableTail = symTable = malloc(sizeof(SymTableEntry));
    } else {
        symTableTail->next = malloc(sizeof(SymTableEntry));
        symTableTail = symTableTail->next;
    }
    symTableTail->start = addr;
    symTableTail->end = addr + size;
    strncpy(symTableTail->name, name, 11);
}

void free_sym_table() {
    SymTableEntry *t;
    while (symTable) {
        t = symTable->next;
        free(symTable);
        symTable = t;
    }
}

static FunTracer *funTracer = NULL; 
static FunTracer *funTracerTail = NULL;

void trace_function(Decode *_this) {
    if (BITS(_this->isa.inst.val, 6, 0) == 0b1101111) {
        if (funTracer == NULL) {
            funTracer = malloc(sizeof(FunTracer));
            funTracerTail = funTracer;
        } else {
            funTracerTail->next = malloc(sizeof(FunTracer));
            funTracerTail = funTracerTail->next;
        }
        funTracerTail->dst = _this->dnpc;
        funTracerTail->pc = _this->pc;
        funTracerTail->type = FUN_CAL;
        funTracerTail->next = NULL;
    } else if (_this->isa.inst.val == 0x00008067) {
        if (funTracer == NULL) {
            funTracer = malloc(sizeof(FunTracer));
            funTracerTail = funTracer;
        } else {
            funTracerTail->next = malloc(sizeof(FunTracer));
            funTracerTail = funTracerTail->next;
        }
        funTracerTail->dst = _this->dnpc;
        funTracerTail->pc = _this->pc;
        funTracerTail->type = FUN_RET;
        funTracerTail->next = NULL;
    }
}

void free_function_tracer() {
    FunTracer *t;
    while (funTracer) {
        t = funTracer->next;
        free(funTracer);
        funTracer = t;
    }
    funTracer = NULL;
}

void function_trace_display() {
    int level = 0;
    FunTracer *node = funTracer;
    while (node) {
        char* symName = "\0";
        SymTableEntry *entry = symTable;
        while (entry)
        {
            if (node->dst >= entry->start && node->dst < entry->end) {
                symName = entry->name;
                break;
            }
            entry = entry->next;
        }
        
        printf(FMT_WORD":", node->pc);
        if (node->type == FUN_CAL) {
            for (int i = 0; i < level; i++) {putchar(' '); putchar(' ');}
            level++;
            printf("call [%s@"FMT_WORD"]\n", symName, node->dst);
        } else {
            if (level > 0) level--;
            for (int i = 0; i < level; i++) {putchar(' '); putchar(' ');}
            printf("ret  [%s@"FMT_WORD"]\n", symName, node->dst);

        }
        node = node->next;
    }
}

static InstTracer instTracer = {};

void trace_ins(Decode *_this) {
    #ifdef CONFIG_ITRACE_COND
    strcpy(instTracer.inst[instTracer.end], _this->logbuf);
    instTracer.end = (instTracer.end + 1) % INST_TRACER_SIZE;
    if (instTracer.end == instTracer.start) instTracer.start = (instTracer.start + 1) % INST_TRACER_SIZE;
    # endif
}

void ins_trace_display() {
  int i = instTracer.start;
  while (i != instTracer.end)
  {
    puts(instTracer.inst[i]);
    i = (i + 1) % INST_TRACER_SIZE;
  }
}

static DeviceTracer deviceTracer = {};

void trace_device(const char *name, paddr_t addr, paddr_t offset, int type, paddr_t pc, int len) {
    deviceTracer.trace[deviceTracer.end] = (DeviceTracerEntry) { .addr = addr, .offset = offset, .type = type, .pc = pc, .len=len};
    strcpy(deviceTracer.trace[deviceTracer.end].name, name);
    
    deviceTracer.end = (deviceTracer.end + 1) % INST_TRACER_SIZE;
    if (deviceTracer.end == deviceTracer.start) deviceTracer.start = (deviceTracer.start + 1) % DEVICE_TRACER_SIZE;
}

void device_trace_display() {
    int i = deviceTracer.start;
    while (i != deviceTracer.end) {
        printf("pc=" FMT_PADDR " ", deviceTracer.trace[i].pc);
        printf(deviceTracer.trace[i].type == DEVICE_READ ? " read" : "write");
        printf(
            " " FMT_PADDR "(%s) offset=" FMT_PADDR " len=%d\n", 
            deviceTracer.trace[i].addr, deviceTracer.trace[i].name, deviceTracer.trace[i].offset, deviceTracer.trace[i].len
            );
        i = (i + 1) % DEVICE_TRACER_SIZE;
    }
}
