module encoder(x, en, y);
    input [7:0] x;
    input en;
    output reg [2:0] y;
    
    integer i;
    always @(en or x)
    begin
        if (en) begin
            y = 0;
            for (i = 0; i<8 ; i = i+1) begin
                if (x[i]) y = i[2:0];
            end
        end
        else y = 0;
    end
endmodule //encoder
