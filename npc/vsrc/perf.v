module PerfCounter(
    input reset,
    input clk,

    input ifu_valid,

    input icache_valid,
    input icache_start,
    input icache_isHit,
    input [31:0] icache_pc,
    
    input lsu_ren,
    input lsu_wen,
    input [31:0] lsu_addr,
    input lsu_isWaiting
);

    import "DPI-C" function void perf_lsu_state_update(input ren, input wen, input waiting, input int addr);
    always @(lsu_isWaiting) begin
        if (!reset) perf_lsu_state_update(lsu_ren, lsu_wen, lsu_isWaiting, lsu_addr);
    end

    import "DPI-C" function void perf_icache_pc_update(input int pc);
    import "DPI-C" function void perf_icache_valid_update(input valid);
    always @(icache_valid or icache_pc) begin
        if (!reset) perf_icache_pc_update(icache_pc);
        if (!reset) perf_icache_valid_update(icache_valid);
    end

    import "DPI-C" function void perf_icache_start_update(input start);
    always @(icache_start) begin
        if (!reset) perf_icache_start_update(icache_start);
    end

    import "DPI-C" function void perf_icache_is_hit_update(input isHit);
    always @(posedge clk) begin
        if (!reset) perf_icache_is_hit_update(icache_isHit);
    end
    
endmodule //PrefCounter
