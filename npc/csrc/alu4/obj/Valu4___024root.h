// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See Valu4.h for the primary calling header

#ifndef VERILATED_VALU4___024ROOT_H_
#define VERILATED_VALU4___024ROOT_H_  // guard

#include "verilated.h"

class Valu4__Syms;

class Valu4___024root final : public VerilatedModule {
  public:

    // DESIGN SPECIFIC STATE
    VL_IN8(in_x,3,0);
    VL_IN8(in_y,3,0);
    VL_IN8(in_s,2,0);
    VL_OUT8(out_s,3,0);
    CData/*3:0*/ alu4__DOT__out_diff;
    CData/*0:0*/ __VactContinue;
    IData/*31:0*/ __VstlIterCount;
    IData/*31:0*/ __VicoIterCount;
    IData/*31:0*/ __VactIterCount;
    VlTriggerVec<1> __VstlTriggered;
    VlTriggerVec<1> __VicoTriggered;
    VlTriggerVec<0> __VactTriggered;
    VlTriggerVec<0> __VnbaTriggered;

    // INTERNAL VARIABLES
    Valu4__Syms* const vlSymsp;

    // CONSTRUCTORS
    Valu4___024root(Valu4__Syms* symsp, const char* v__name);
    ~Valu4___024root();
    VL_UNCOPYABLE(Valu4___024root);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
