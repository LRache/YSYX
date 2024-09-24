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
    input gpr_wen,

    input [31:0] csr_waddr,
    input [31:0] csr_wdata,
    input csr_wen
);
    import "DPI-C" function void interface_ebreak();
    import "DPI-C" function void interface_ivd_inst();
    import "DPI-C" function void interface_update_gpr(input int addr, input int data);
    import "DPI-C" function void interface_update_csr(input int addr, input int data);
    import "DPI-C" function void interface_update_reset(input reset);
    import "DPI-C" function void interface_update_pc(input int pc);
    import "DPI-C" function void interface_update_inst(input int inst);
    import "DPI-C" function void interface_update_done(input done);
   
    always @(posedge clk) 
    begin
        interface_update_done(done);
        if (!reset) interface_update_pc(pc);
        if (!reset) interface_update_inst(inst);
        if (brk) interface_ebreak();
        if (ivd) interface_ivd_inst();
        if (gpr_wen) interface_update_gpr(gpr_waddr, gpr_wdata);
        if (csr_wen) interface_update_csr(csr_waddr, csr_wdata); 
        interface_update_reset(reset);
    end

    // always @(reset)
    // begin
    //     interface_update_reset(reset);
    // end
    
    // always @(pc)
    // begin
    //     if (!reset) interface_update_pc(pc);
    // end

    // always @(inst)
    // begin
    //     interface_update_inst(inst);
    // end

    // always @(done)
    // begin
    //     interface_update_done(done);
    // end

endmodule //Dbg

