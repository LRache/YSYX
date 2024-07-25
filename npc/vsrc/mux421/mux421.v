module mux421(y, x0, x1, x2, x3, f);
    input [1:0] y;
    input [1:0] x0;
    input [1:0] x1;
    input [1:0] x2;
    input [1:0] x3;
    output reg [1:0] f;

    always @ (y or x0 or x1 or x2 or x3)
    begin
        case (y)
            2'b00: f = x0;
            2'b01: f = x1;
            2'b10: f = x2;
            2'b11: f = x3;
            default: f = 2'b11;
        endcase
    end
    
endmodule //mux421