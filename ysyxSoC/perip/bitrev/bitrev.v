module bitrev (
  input  sck,
  input  ss,
  input  mosi,
  output miso
);
  localparam IDLE = 0;
  localparam RECV = 1;
  localparam SEND = 2;

  reg [7:0] data;
  reg [3:0] state = IDLE;
  reg [2:0] count = 3'h0;
  reg dout = 1'b1;

  assign miso = dout;
  
  always @(posedge sck) begin
    if (!ss) begin
      case (state)
        IDLE: begin
          state = RECV;
          count = 3'h1;
          data[0] = mosi;
          dout = 1'b0;
        end
        RECV: begin
          data[count] = mosi;
          if (count == 3'h7) begin
            state = SEND;
            dout = data[0];
            count = 3'h1;
          end else begin
            count = count + 1;
            dout = 1'b1;
          end
        end
        SEND: begin
          dout = data[count];
          if (count == 3'h7) begin
            state = IDLE;
          end
          count = count + 1;
        end
      endcase
    end else begin
        dout = 1'b1;
    end
  end
endmodule