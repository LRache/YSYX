module Dbg(
    input clk,
    input reset,
    input brk,
    input ivd,
    input [31:0] pc,
    input [31:0] inst,
    input done
);
    import "DPI-C" function void env_break();
    import "DPI-C" function void invalid_inst();
    import "DPI-C" function void update_reset(input byte reset);
    import "DPI-C" function void update_pc(input int pc);
    import "DPI-C" function void update_inst(input int inst);
    import "DPI-C" function void update_valid(input byte valid);

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

endmodule //Dbg

