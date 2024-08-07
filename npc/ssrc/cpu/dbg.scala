package cpu

import chisel3._

class Dbg extends BlackBox {
    val io = IO(new Bundle {
        val clk         = Input(Clock())
        val reset       = Input(Reset())
        val is_ebreak   = Input(Bool())
        val is_invalid  = Input(Bool())
        val pc          = Input(UInt(32.W))
        val inst        = Input(UInt(32.W))
        val valid       = Input(Bool())
    })
}
