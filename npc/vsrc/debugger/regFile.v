import "DPI-C" function void set_reg(input int addr, input int data);

module RegFileDebugger(
    input clk,
    input [4:0]  waddr,
    input [31:0] wdata,
    input wen
);
    always @(posedge clk) begin
        if (wen && waddr != 0) begin
            set_reg({27'b0, waddr}, wdata);
        end
    end
endmodule //RegisterFileDebugger
