module SDRAMBase(
    input clk,
    input [1:0] bank,
    input [12:0] row,
    input [8:0] col,
    input wen,
    input [15:0] wdata,
    input [1:0] wmask,
    input ren,
    output [15:0] rdata
);
    import "DPI-C" function void sdram_read (input int bank, input int row, input int column, output int data);
    import "DPI-C" function void sdram_write(input int bank, input int row, input int column, input  int data, input byte mask);
    
    wire [31:0] r;
    assign rdata = r[15:0];
    
    always @(posedge clk) begin
        if (wen) begin
            sdram_write({30'b0, bank}, {19'b0, row}, {23'b0, col}, {16'b0, wdata}, {6'b0, wmask});
        end
        if (ren) begin
            sdram_read({30'b0, bank}, {19'b0, row}, {23'b0, col}, r);
        end
    end
    
endmodule //SDRAMBase

