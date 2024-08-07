package cpu.reg

import chisel3._

object RegWSel extends Enumeration {
    type RegWSel = Value
    val DIS, EXU, SN, MEM, CSR = Value
}

class RegFileDebugger extends BlackBox {
    val io = IO(new Bundle {
        val clk     = Input(Clock())
        val waddr   = Input(UInt(5.W))
        val wdata   = Input(UInt(32.W))
        val wen     = Input(Bool())
    })
}

class RegFile extends Module {
    val io = IO(new Bundle {
        val waddr   = Input (UInt(5.W))
        val wdata   = Input (UInt(32.W))
        val wen     = Input (Bool())
        val raddr1  = Input (UInt(5.W))
        val raddr2  = Input (UInt(5.W))
        val rdata1  = Output(UInt(32.W))
        val rdata2  = Output(UInt(32.W))
    })

    val registers = Reg(Vec(32, UInt(32.W)))

    // init
    when (reset.asBool) {
        registers.foreach(_ := 0.U)
    }

    when (io.wen & ~(io.waddr === 0.U(5.W))) {
        registers(io.waddr) := io.wdata
    }

    io.rdata1 := registers(io.raddr1)
    io.rdata2 := registers(io.raddr2)

    val debugger = Module(new RegFileDebugger())
    debugger.io.clk   := clock
    debugger.io.waddr := io.waddr
    debugger.io.wdata := io.wdata
    debugger.io.wen   := io.wen
}
