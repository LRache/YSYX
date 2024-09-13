package cpu

import chisel3._

class RegDbgMessage extends Bundle {
    val waddr = Input(UInt(32.W))
    val wen   = Input(Bool())
    val wdata = Input(UInt(32.W))
}

class Dbg extends BlackBox {
    val io = IO(new Bundle {
        val clk   = Input(Clock())
        val reset = Input(Reset())
        val brk   = Input(Bool())
        val ivd   = Input(Bool())
        val pc    = Input(UInt(32.W))
        val inst  = Input(UInt(32.W))
        val done  = Input(Bool())

        val gpr = new RegDbgMessage
        val csr = new RegDbgMessage
    })
}
