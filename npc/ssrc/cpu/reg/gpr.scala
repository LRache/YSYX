package cpu.reg

import chisel3._
import chisel3.util.MuxLookup

import cpu.Config
import chisel3.util.RegEnable

object GPRWSel {
    val SNPC = 0b00
    val CSR  = 0b01
    val EXU  = 0b10
    val MEM  = 0b11
    // 1st Select in EXU: snpc and csr
    // 2nd Select in LSU: exu result and mem
}

class GPRDebugger(addrLength : Int) extends BlackBox {
    val io = IO(new Bundle {
        val clk     = Input(Clock())
        val waddr   = Input(UInt(32.W))
        val wdata   = Input(UInt(32.W))
        val wen     = Input(Bool())
    })
}

class GPR(addrLength : Int) extends Module {
    val io = IO(new Bundle {
        val waddr   = Input (UInt(addrLength.W))
        val wdata   = Input (UInt(32.W))
        val wen     = Input (Bool())
        val raddr1  = Input (UInt(addrLength.W))
        val raddr2  = Input (UInt(addrLength.W))
        val rdata1  = Output(UInt(32.W))
        val rdata2  = Output(UInt(32.W))
    })
    val gprCount = (1 << addrLength) - 1

    val raddr1 = io.raddr1
    val raddr2 = io.raddr2
    val waddr  = io.waddr 

    // val registers = RegInit(VecInit(Seq.fill(gprCount)(0.U(32.W))))
    // for (i <- 0 to gprCount - 1) {
    //     registers(i) := Mux((waddr === (i+1).U && io.wen), io.wdata, registers(i))
    // }
    val gpr = VecInit((0 to gprCount - 1).map(i => RegEnable(io.wdata, 0.U, (waddr === (i+1).U && io.wen))))
    val table: Seq[(UInt, UInt)] = (for (i <- 0 to gprCount - 1) yield((i+1).U, gpr(i)))
    io.rdata1 := Mux(raddr1.orR, MuxLookup(raddr1, 0.U)(table), 0.U)
    io.rdata2 := Mux(raddr2.orR, MuxLookup(raddr2, 0.U)(table), 0.U)

    val debugger = Module(new GPRDebugger(addrLength))
    debugger.io.clk   := clock
    debugger.io.waddr := io.waddr
    debugger.io.wdata := io.wdata
    debugger.io.wen   := io.wen
}
