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

        val gpr = Flipped(new RegWIO(32))
        val csr = Flipped(new RegWIO(32))
        
        val is_trap = Input(Bool())
        val epc = Input(UInt(32.W))
        val cause = Input(UInt(32.W))

        val exu_valid = Input(Bool())

        val branch_predict_failed = Input(Bool())
        val branch_predict_success = Input(Bool())
    })
}
