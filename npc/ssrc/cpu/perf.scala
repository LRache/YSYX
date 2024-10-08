package cpu

import chisel3._

class ICachePerfCounter extends Bundle {
    val valid = Output(Bool())
    val isHit = Output(Bool())
    val start = Output(Bool())
    val pc    = Output(UInt(32.W))
}

class LSUPerfCounter extends Bundle {
    val ren = Output(Bool())
    val wen = Output(Bool())
    val addr = Output(UInt(32.W))
    val isWaiting = Output(Bool())
}

class PerfCounter extends BlackBox {
    val io = IO(new Bundle {
        val reset = Input(Bool())
        val clk   = Input(Bool())

        val ifu_valid = Input(Bool())
        val idu_ready = Input(Bool())
        val exu_valid = Input(Bool())
        
        val icache = Flipped(new ICachePerfCounter)
        val lsu = Flipped(new LSUPerfCounter)
    })
}
