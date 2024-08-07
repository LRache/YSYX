import "DPI-C" function void env_break();
import "DPI-C" function void invalid_inst();
import "DPI-C" function void update_reset(input byte reset);
import "DPI-C" function void update_pc(input int pc);
import "DPI-C" function void update_inst(input int inst);
import "DPI-C" function void update_valid(input byte valid);

module Dbg(
    input clk,
    input reset,
    input is_ebreak,
    input is_invalid,
    input [31:0] pc,
    input [31:0] inst,
    input valid
);
    always @(posedge clk) 
    begin
        if (is_ebreak)  env_break();
        if (is_invalid) invalid_inst();
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

    always @(valid)
    begin
        update_valid({7'b0, valid});
    end

endmodule //Dbg

