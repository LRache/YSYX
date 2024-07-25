module alu4(in_x, in_y, in_s, out_s);
    input  [3:0] in_x;
    input  [3:0] in_y;
    input  [2:0] in_s;
    output reg [3:0] out_s;

    wire [3:0] out_sum;
    wire [3:0] out_diff;
    wire [3:0] out_not;
    wire [3:0] out_and;
    wire [3:0] out_or;
    wire [3:0] out_xor;
    wire out_equal;
    wire out_less;
    wire c0;
    wire c1;
    wire [3:0] z;
    wire overflow;

    assign {c0, out_sum} = in_x + in_y;
    assign z = ~in_y + 1;
    assign {c1, out_diff} = in_x + z;
    assign overflow = (in_y[3] != in_x[3]) && (in_x[3] != out_diff[3]);
    assign out_equal = ~(| out_diff);
    assign out_less = out_diff[3] ^ overflow;
    assign out_not = ~in_x;
    assign out_and = in_x & in_y;
    assign out_or = in_x | in_y;
    assign out_xor = in_x ^ in_y;

    always @(*) 
    begin
        out_s = 4'b0000;
        case(in_s)
            3'b000: out_s    = out_sum;
            3'b001: out_s    = out_diff;
            3'b010: out_s    = out_not;
            3'b011: out_s    = out_and;
            3'b100: out_s    = out_or;
            3'b101: out_s    = out_xor;
            3'b110: out_s[0] = out_less;
            3'b111: out_s[0] = out_equal;
        endcase
    end
    
endmodule