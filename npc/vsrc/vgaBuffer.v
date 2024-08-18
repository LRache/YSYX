module VGABuffer(
    input [31:0] wdata,
    input [31:0] waddr,
    input [3:0]  wmask,
    input wen,

    input [31:0] rx,
    input [31:0] ry,
    output [31:0] rdata,
    input ren
);
    import "DPI-C" function void vga_buffer_read (input int x, input int y, output int data);
    import "DPI-C" function void vga_buffer_write(input int waddr, input int wdata, input byte mask);

    always @(wen) begin
        if (wen) begin
            vga_buffer_write(waddr, wdata, {4'b0, wmask});
        end
    end

    always @(ren or rx or ry) begin
        if (ren) begin
            vga_buffer_read(rx, ry, rdata);
        end
    end
    
endmodule //VGABuffer
