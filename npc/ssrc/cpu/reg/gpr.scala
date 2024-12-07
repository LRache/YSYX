package cpu.reg

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.RegEnable

import cpu.Config

class GPR(addrWidth : Int) extends Module {
    val io = IO(new Bundle {
        val waddr   = Input (UInt(addrWidth.W))
        val wdata   = Input (UInt(32.W))
        val wen     = Input (Bool())
        val raddr1  = Input (UInt(addrWidth.W))
        val raddr2  = Input (UInt(addrWidth.W))
        val rdata1  = Output(UInt(32.W))
        val rdata2  = Output(UInt(32.W))
    })
    val gprCount = (1 << addrWidth) - 1

    val raddr1 = io.raddr1
    val raddr2 = io.raddr2
    val waddr  = io.waddr 

    val gpr = VecInit((0 to gprCount - 1).map(i => RegEnable(io.wdata, (waddr === (i+1).U && io.wen))))
    val table: Seq[(UInt, UInt)] = ((for (i <- 0 to gprCount - 1) yield((i+1).U, gpr(i))))
    io.rdata1 := Mux(raddr1.orR, MuxLookup(raddr1, 0.U)(table), 0.U)
    io.rdata2 := Mux(raddr2.orR, MuxLookup(raddr2, 0.U)(table), 0.U)
}
