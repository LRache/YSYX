// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See Vtop.h for the primary calling header

#ifndef VERILATED_VTOP___024ROOT_H_
#define VERILATED_VTOP___024ROOT_H_  // guard

#include "verilated.h"

class Vtop__Syms;

class Vtop___024root final : public VerilatedModule {
  public:

    // DESIGN SPECIFIC STATE
    VL_IN8(ps2_clk,0,0);
    VL_IN8(ps2_dat,0,0);
    VL_OUT8(out_seg0,6,0);
    VL_OUT8(out_seg1,6,0);
    VL_OUT8(out_seg2,6,0);
    VL_OUT8(out_seg3,6,0);
    VL_OUT8(out_seg4,6,0);
    VL_OUT8(out_seg5,6,0);
    CData/*3:0*/ top__DOT__n;
    CData/*3:0*/ top__DOT__seg_dat0;
    CData/*3:0*/ top__DOT__seg_dat1;
    CData/*7:0*/ top__DOT__ascii_dat;
    CData/*7:0*/ top__DOT__count;
    CData/*0:0*/ top__DOT__b;
    CData/*0:0*/ __Vtrigrprev__TOP__ps2_clk;
    CData/*0:0*/ __VactContinue;
    SData/*10:0*/ top__DOT__dat;
    IData/*31:0*/ __VstlIterCount;
    IData/*31:0*/ __VactIterCount;
    VlUnpacked<CData/*0:0*/, 2> __Vm_traceActivity;
    VlTriggerVec<1> __VstlTriggered;
    VlTriggerVec<1> __VactTriggered;
    VlTriggerVec<1> __VnbaTriggered;

    // INTERNAL VARIABLES
    Vtop__Syms* const vlSymsp;

    // CONSTRUCTORS
    Vtop___024root(Vtop__Syms* symsp, const char* v__name);
    ~Vtop___024root();
    VL_UNCOPYABLE(Vtop___024root);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
