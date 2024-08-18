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

    val raddr1 = io.raddr1(3,0)
    val raddr2 = io.raddr2(3,0)
    val waddr  = io.waddr (3,0)

    val registers = RegInit(VecInit(Seq.fill(16)(0.U(32.W))))
    registers(waddr) := Mux(io.waddr.orR && io.wen, io.wdata, registers(waddr))
    io.rdata1 := registers(raddr1)
    io.rdata2 := registers(raddr2)

    val debugger = Module(new RegFileDebugger())
    debugger.io.clk   := clock
    debugger.io.waddr := io.waddr
    debugger.io.wdata := io.wdata
    debugger.io.wen   := io.wen
}
