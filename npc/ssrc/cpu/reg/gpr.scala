package cpu.reg

import chisel3._
import chisel3.util.MuxLookup

import cpu.Config

object GPRWSel extends Enumeration {
    type GPRWSel = Value
    val EXU, SN, MEM, CSR = Value
}

class RegFileDebugger extends BlackBox {
    val io = IO(new Bundle {
        val clk     = Input(Clock())
        val waddr   = Input(UInt(Config.GPRAddrLength.W))
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

    val raddr1 = io.raddr1(addrLength - 1, 0)
    val raddr2 = io.raddr2(addrLength - 1, 0)
    val waddr  = io.waddr (addrLength - 1, 0)

    val registers = RegInit(VecInit(Seq.fill(gprCount)(0.U(32.W))))
    for (i <- 0 to gprCount - 1) {
        registers(i) := Mux((waddr === (i+1).U && io.wen), io.wdata, registers(i))
    }
    val table: Seq[(UInt, UInt)] = (for (i <- 0 to gprCount - 1) yield((i+1).U, registers(i)))
    io.rdata1 := Mux(raddr1.orR, MuxLookup(raddr1, 0.U)(table), 0.U)
    io.rdata2 := Mux(raddr2.orR, MuxLookup(raddr2, 0.U)(table), 0.U)

    val debugger = Module(new RegFileDebugger())
    debugger.io.clk   := clock
    debugger.io.waddr := io.waddr
    debugger.io.wdata := io.wdata
    debugger.io.wen   := io.wen
}
