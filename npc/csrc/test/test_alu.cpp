#ifdef TOP_ALU

#include <stdio.h>
#include "debug.h"
#include "VAlu/VAlu.h"

typedef int (*ans_t)(int, int);

int add(int a, int b) {
    return a + b;
}

int sll(int a, int b) {
    return a << (b & 0b11111);
}

int slt(int a, int b) {
    return a < b;
}

int sltu(int a, int b) {
    return (unsigned int) a < b;
}

int zero(int a, int b) {
    return 0;
}

int _xor(int a, int b) {
    return a ^ b;
}

int srl(int a, int b) {
    return (unsigned int)a >> (b & 0b11111);
}

int _or(int a, int b) {
    return a | b;
}

int _and(int a, int b) {
    return a & b;
}

int mul(int a, int b) {
    return a * b;
}

int mulh(int a, int b) {
    int64_t t = (int64_t)a * b;
    return t >> 32;
}

int mulhu(int a, int b) {
    uint64_t t = (uint64_t)(uint32_t)a * (uint64_t)(uint32_t)b;
    return (t >> 32) & 0xffffffff;
}

int sub(int a, int b) {
    return a - b;
}

int sra(int a, int b) {
    return a >> (b & 0b11111);
}

int bsel(int a, int b) {
    return b;
}

void test_alu() {
    VAlu alu;
    ans_t ans[16] = {add, sub, _and, _or, _xor, sll, srl, sra, slt, sltu, bsel, mul, mulh, mulhu};
    for (int a = -32; a < 32; a++) {
        for (int b = -32; b < 32; b++) {
            for (int s = 0; s < 11; s++) {
                alu.io_sel = s;
                alu.io_a = a;
                alu.io_b = b;
                alu.eval();
                int answer = ans[s](a, b);
                Assert(alu.io_result == answer, "a=%d b=%d r=%08x s=%d ans=%d", a, b, alu.io_result, s, answer);
            }
        }
    }
    Log("ALU test pass.");
}

#endif
