module PerfCounter(
    input ifu_valid
);

    import "DPI-C" function void perf_ifu_valid_update(input valid);
    always @(ifu_valid) begin
        perf_ifu_valid_update(ifu_valid);
    end
    
endmodule //PrefCounter
