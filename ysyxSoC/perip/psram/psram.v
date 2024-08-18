import "DPI-C" function void psram_read (input int addr, output byte data, input int count);
import "DPI-C" function void psram_write(input int addr, input  byte data, input int count);

module psram(
  input sck,
  input ce_n,
  inout [3:0] dio
);
  wire reset = ce_n;

  typedef enum [2:0] { s_cmd, s_addr, s_recv, s_delay, s_send, s_ivd } state_t;
  reg [2:0] state;
  reg [7:0] rdata;
  reg [3:0] wdata;
  reg [23:0] addr;
  reg [7:0] cmd;
  reg [4:0] counter;
  
  assign dio = state == s_send ? rdata[3:0] : 4'bz;

  always @(posedge sck or posedge reset) begin
      if (reset) begin 
        state <= s_cmd;
        counter <= 5'd0;
      end
      else begin
        if (state == s_cmd) begin
            state   <= (counter == 5'd7) ? s_addr  : s_cmd;
            counter <= (counter == 5'd7) ? 5'd0    : counter + 5'd1;
        end else if (state == s_addr) begin
            state   <= (counter == 5'd5) ? ((cmd == 8'h38) ? s_recv : ((cmd == 8'hEB) ? s_delay : s_ivd)) : s_addr;
            counter <= (counter == 5'd5) ? 5'd0 : counter + 5'd1;
        end else if(state == s_recv) begin
            counter <= counter + 5'd1;
        end else if (state == s_delay) begin
            state   <= (counter == 5'd5) ? s_send : s_delay;
            counter <= (counter == 5'd5) ? 5'd0 : counter + 5'd1;
        end else if(state == s_send) begin
            counter <= counter + 5'd1;
        end else if (state == s_ivd) begin
            $fwrite(32'h80000002, "Assertion failed: Unsupport command `%xh`\n", cmd);
            $fatal;
        end
      end
  end

  always @(posedge sck or posedge reset) begin
    if (reset) cmd <= 8'h0;
    else if (state == s_cmd) begin
        cmd <= {cmd[6:0], dio[0]};
        counter <= counter + 5'd1;
    end
  end

  always @(posedge sck or posedge reset) begin
      if (reset) addr <= 24'h0;
      else if (state == s_addr) begin
          addr <= {addr[19:0], dio};
          counter <= counter + 5'd1;
      end
  end

  always @(posedge sck or posedge reset) begin
      if (reset) wdata <= 4'h0;
      else if (state == s_recv) begin
          psram_write({8'h0, addr}, {4'h0, dio}, {27'd0, counter});
      end
  end

  always @(posedge sck or posedge reset) begin
      if (reset) rdata = 8'h0;
      else if (state == s_send) begin
          psram_read({8'h0, addr}, rdata, {27'd0, counter});
      end
  end

endmodule
