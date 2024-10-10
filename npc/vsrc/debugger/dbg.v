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
    input csr_wen,

    input [31:0] epc,
    input [31:0] cause,
    input is_trap,

    input exu_valid,

    input branch_predict_failed,
    input branch_predict_success
);
    import "DPI-C" function void interface_ebreak();
    import "DPI-C" function void interface_ivd_inst();
    import "DPI-C" function void interface_update_gpr(input int addr, input int data);
    import "DPI-C" function void interface_update_csr(input int addr, input int data);
    import "DPI-C" function void interface_update_reset(input reset);
    import "DPI-C" function void interface_update_pc(input int pc);
    import "DPI-C" function void interface_update_inst(input int inst);
    import "DPI-C" function void interface_update_done(input done);

    import "DPI-C" function void interface_update_exu_valid(input valid);
    import "DPI-C" function void interface_update_branch_predict_failed(input predict_failed);
    import "DPI-C" function void interface_update_branch_predict_success(input predict_success);
   
    always @(posedge clk) 
    begin
        if (!reset && done) interface_update_pc(pc);
        if (!reset && done) interface_update_inst(inst);
        if (brk && done) interface_ebreak();
        if (ivd && done) interface_ivd_inst();
        if (gpr_wen) interface_update_gpr(gpr_waddr, gpr_wdata);
        if (csr_wen) interface_update_csr(csr_waddr, csr_wdata);
        if (is_trap) interface_update_csr(32'h7, cause);
        if (is_trap) interface_update_csr(32'h6, epc);
        interface_update_reset(reset);
        interface_update_done(done);

        if (!reset) interface_update_exu_valid(exu_valid);
        if (!reset) interface_update_branch_predict_failed(branch_predict_failed);
        if (!reset) interface_update_branch_predict_success(branch_predict_success);
    end
endmodule //Dbg

