// module Clint(
//   input clk,
//   input reset,
//   input raddr,
//   output[31:0] rdata
// );
//   reg [31:0] counter0;
//   reg [31:0] counter1;
//   assign rdata = raddr ? counter1 : counter0;
//   always @(posedge clk) begin
//       if (reset) begin
//         counter0 <= 0;
//         counter1 <= 0;
//       end
//       else begin
//         counter0 <= counter0 + 1;
//         if ((&counter0)) counter1 <= counter1 + 1;
//       end
//   end
    
// endmodule //Clint