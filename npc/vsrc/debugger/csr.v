import "DPI-C" function void set_csr(input int addr, input int data);

module CSRDebugger(
    input clk,
    input wen,
    input [11:0] waddr,
    input [31:0] wdata
);
    always @(posedge clk) begin
        if (wen) begin
            set_csr({20'b0, waddr}, wdata);
        end
    end
    
endmodule //CSRDebugger
