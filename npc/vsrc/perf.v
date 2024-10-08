module PerfCounter(
    input reset,
    input clk,

    input ifu_valid,
    input idu_ready,
    input exu_valid,

    input icache_valid,
    input icache_start,
    input icache_isHit,
    input [31:0] icache_pc,
    
    input lsu_ren,
    input lsu_wen,
    input [31:0] lsu_addr,
    input lsu_isWaiting

    // input branch_predict_failed,
    // input branch_predict_success
);

    import "DPI-C" function void interface_update_lsu_state(input ren, input wen, input waiting, input int addr);
    always @(lsu_isWaiting) begin
        if (!reset) interface_update_lsu_state(lsu_ren, lsu_wen, lsu_isWaiting, lsu_addr);
    end

    import "DPI-C" function void interface_update_icache_pc(input int pc);
    import "DPI-C" function void interface_update_icache_mem_valid(input valid);
    always @(icache_valid or icache_pc) begin
        if (!reset) interface_update_icache_pc(icache_pc);
        if (!reset) interface_update_icache_mem_valid(icache_valid);
    end

    import "DPI-C" function void interface_update_icache_mem_start(input start);
    always @(icache_start) begin
        if (!reset) interface_update_icache_mem_start(icache_start);
    end

    import "DPI-C" function void interface_update_icache_hit(input isHit);
    import "DPI-C" function void interface_update_idu_ready(input ready);
    always @(posedge clk) begin
        if (!reset) interface_update_idu_ready(idu_ready);
        if (!reset) interface_update_icache_hit(icache_isHit);
    end

    // import "DPI-C" function void interface_update_exu_valid(input valid);
    // import "DPI-C" function void interface_update_branch_predict_failed(input predict_failed);
    // import "DPI-C" function void interface_update_branch_predict_success(input predict_success);
    // always @(posedge clk) begin
    //     if (!reset) interface_update_exu_valid(exu_valid);
    //     if (!reset) interface_update_branch_predict_failed(branch_predict_failed);
    //     if (!reset) interface_update_branch_predict_success(branch_predict_success);
    // end
    
endmodule //PrefCounter
