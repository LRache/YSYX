// import "DPI-C" function void pmem_read (input int addr, output int word, input int  size);
// import "DPI-C" function void pmem_write(input int addr, input  int word, input byte wmask);

module Memory(
    input  [31:0] waddr,
    input  [7:0]  wmask,
    input  [31:0] wdata,
    input  wen,
    input  [31:0] raddr,
    output [31:0] rdata,
    input  [2:0] rsize,
    input  ren,
    input  reset
);
    always @(waddr or raddr or wen or ren or reset) begin
        if (!reset && wen) begin
            // pmem_write(waddr, wdata, wmask);
        end
        if (!reset && ren) begin
            // pmem_read(raddr, rdata, {29'b0, rsize});
        end
    end
endmodule
