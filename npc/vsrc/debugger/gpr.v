module GPRDebugger(
    input clk,
    input [31:0]  waddr,
    input [31:0] wdata,
    input wen
);
    import "DPI-C" function void set_reg(input int addr, input int data);

    always @(posedge clk) begin
        if (wen && waddr != 0) begin
            set_reg(waddr, wdata);
        end
    end
endmodule //RegisterFileDebugger
