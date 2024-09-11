module CSRDebugger(
    input clk,
    input wen,
    input [11:0] waddr,
    input [31:0] wdata
);
    
endmodule //CSRDebugger

module PerfCounter(
    input reset,

    input ifu_valid,

    input icache_valid,
    input icache_start,
    input icache_isHit,
    
    input lsu_ren,
    input lsu_wen,
    input [31:0] lsu_addr,
    input lsu_isWaiting
);
    
endmodule //PrefCounter

module Dbg(
    input clk,
    input reset,
    input brk,
    input ivd,
    input [31:0] pc,
    input [31:0] inst,
    input done,

    input [31:0] gpr_waddr,
    input [31:0] gpr_wdata,
    input gpr_wen
);

endmodule //Dbg

