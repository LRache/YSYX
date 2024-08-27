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

    import "DPI-C" function void perf_ifu_valid_update(input valid);
    always @(ifu_valid) begin
        if (!reset) perf_ifu_valid_update(ifu_valid);
    end

    import "DPI-C" function void perf_lsu_state_update(input ren, input wen, input waiting, input int addr);
    always @(lsu_isWaiting) begin
        if (!reset) perf_lsu_state_update(lsu_ren, lsu_wen, lsu_isWaiting, lsu_addr);
    end

    import "DPI-C" function void perf_icache_valid_update(input valid);
    always @(icache_valid) begin
        if (!reset) perf_icache_valid_update(icache_valid);
    end

    import "DPI-C" function void perf_icache_start_update(input start);
    always @(icache_start) begin
        if (!reset) perf_icache_start_update(icache_start);
    end

    import "DPI-C" function void perf_icache_is_hit_update(input isHit);
    always @(icache_isHit) begin
        if (!reset) perf_icache_is_hit_update(icache_isHit);
    end
    
endmodule //PrefCounter
