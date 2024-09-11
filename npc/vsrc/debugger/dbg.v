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
    import "DPI-C" function void env_break();
    import "DPI-C" function void invalid_inst();
    import "DPI-C" function void update_reset(input byte reset);
    import "DPI-C" function void update_pc(input int pc);
    import "DPI-C" function void update_inst(input int inst);
    import "DPI-C" function void update_valid(input byte valid);
    import "DPI-C" function void set_reg(input int addr, input int data);

    always @(posedge clk) 
    begin
        if (brk) env_break();
        if (ivd) invalid_inst();
    end

    always @(reset)
    begin
        update_reset({7'b0, reset});
    end
    
    always @(pc)
    begin
        update_pc(pc);
    end

    always @(inst)
    begin
        update_inst(inst);
    end

    always @(done)
    begin
        update_valid({7'b0, done});
    end

    always @(posedge clk) begin
        if (gpr_wen) begin
            set_reg(gpr_waddr, gpr_wdata);
        end
    end

endmodule //Dbg

