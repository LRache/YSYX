package cpu

import chisel3._

class Dbg extends BlackBox {
    val io = IO(new Bundle {
        val clk   = Input(Clock())
        val reset = Input(Reset())
        val brk   = Input(Bool())
        val ivd   = Input(Bool())
        val pc    = Input(UInt(32.W))
        val inst  = Input(UInt(32.W))
        val done  = Input(Bool())

        val gpr_waddr = Input(UInt(32.W))
        val gpr_wdata = Input(UInt(32.W))
        val gpr_wen   = Input(Bool())
    })
}
