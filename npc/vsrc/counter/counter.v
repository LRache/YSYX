module counter(clk);
    input clk;

    reg [31:0] timer;
    reg [7:0] count

    always @(posedge clk)
    begin
        timer <= timer + 1;
        if (timer == 24999999)
        begin
            count <= count + 1;
        end
    end
endmodule