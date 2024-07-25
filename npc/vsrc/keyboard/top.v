module top(
    input ps2_dat,
    input ps2_clk,
    output [6:0] out_seg0,
    output [6:0] out_seg1,
    output [6:0] out_seg2,
    output [6:0] out_seg3,
    output [6:0] out_seg4,
    output [6:0] out_seg5
);
    reg [3:0] n;
    reg [10:0] dat;
    reg [3:0] seg_dat0;
    reg [3:0] seg_dat1;
    reg [7:0] ascii_dat;
    reg [7:0] count;
    reg b;

    bcd7seg bcd7seg0 (
        .b (seg_dat0[3:0]),
        .h (out_seg0[6:0])
    );

    bcd7seg bcd7seg1 (
        .b (seg_dat1[3:0]),
        .h (out_seg1[6:0])
    );

    bcd7seg bcd7seg2 (
        .b (ascii_dat[3:0]),
        .h (out_seg2[6:0])
    );

    bcd7seg bcd7seg3 (
        .b (ascii_dat[7:4]),
        .h (out_seg3[6:0])
    );

    bcd7seg bcd7seg4 (
        .b (count[3:0]),
        .h (out_seg4[6:0])
    );

    bcd7seg bcd7seg5 (
        .b (count[7:4]),
        .h (out_seg5[6:0])
    );

    initial
    begin
        n = 4'b0;
        count = 8'b0;
    end
    
    always @(negedge ps2_clk)
    begin
        dat[n] <= ps2_dat;
        n <= n + 4'b1;
        if (n == 10)
        begin
            n <= 4'b0;
            if ((dat[0] == 0) && (dat[10]) && (^dat[9:1]))
            begin
                seg_dat0 <= dat[4:1];
                seg_dat1 <= dat[8:5];
                case (dat[8:1])
                    8'h45: ascii_dat = 8'h30; // 0
                    8'h16: ascii_dat = 8'h31;
                    8'h1e: ascii_dat = 8'h32;
                    8'h26: ascii_dat = 8'h33;
                    8'h25: ascii_dat = 8'h34;
                    8'h2e: ascii_dat = 8'h35;
                    8'h36: ascii_dat = 8'h36;
                    8'h3d: ascii_dat = 8'h37;
                    8'h3e: ascii_dat = 8'h38;
                    8'h46: ascii_dat = 8'h39;
                    8'h1c: ascii_dat = 8'h41; // A
                    8'h32: ascii_dat = 8'h42;
                    8'h21: ascii_dat = 8'h43;
                    8'h23: ascii_dat = 8'h44;
                    8'h24: ascii_dat = 8'h45;
                    8'h2b: ascii_dat = 8'h46;
                    8'h34: ascii_dat = 8'h47;
                    8'h33: ascii_dat = 8'h48;
                    8'h43: ascii_dat = 8'h49;
                    8'h3b: ascii_dat = 8'h4a;
                    8'h42: ascii_dat = 8'h4b;
                    8'h4b: ascii_dat = 8'h4c;
                    8'h3a: ascii_dat = 8'h4d;
                    8'h31: ascii_dat = 8'h4e;
                    8'h44: ascii_dat = 8'h4f;
                    8'h4d: ascii_dat = 8'h50;
                    8'h15: ascii_dat = 8'h51;
                    8'h2d: ascii_dat = 8'h52;
                    8'h1b: ascii_dat = 8'h53;
                    8'h2c: ascii_dat = 8'h54;
                    8'h3c: ascii_dat = 8'h55;
                    8'h2a: ascii_dat = 8'h56;
                    8'h1d: ascii_dat = 8'h57;
                    8'h22: ascii_dat = 8'h58;
                    8'h35: ascii_dat = 8'h59;
                    8'h1a: ascii_dat = 8'h5a;
                    default: ascii_dat = 8'b0;
                endcase
                if (dat[8:1] == 8'hf0)
                begin
                    b = 1'b1;
                end
                else
                begin
                    if (b == 1'b1)
                    begin
                        count = count + 1'b1;
                        b = 1'b0;
                    end
                end
            end
        end
    end
endmodule