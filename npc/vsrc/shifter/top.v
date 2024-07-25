module top(
    input clk,
    output [13:0] seg
);
    reg [7:0] q;
    initial
    begin
        q = 8'b10101010;
    end
    
    bcd7seg bcd7seg0(
        .b (q[3:0]),
        .h (seg[6:0])
    );
    bcd7seg bcd7seg1(
        .b (q[7:4]),
        .h (seg[13:7])
    );
    
    wire t;
    assign t = q[0] ^ q[2] ^ q[3] ^ q[4];
    
    always @(posedge clk) 
    begin
        q <= {t, q[7:1]};
        if (~(| q))
        begin
            q <= 8'b10101010;
        end
    end

endmodule