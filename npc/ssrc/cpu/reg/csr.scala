package cpu.reg

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.MuxCase

object CSRWSel extends Enumeration {
    type CSRWSel = Value
    val W, S, C, WI, SI, CI = Value
}

object CSRAddr {
    val MVENDORID= 0x100.U(12.W)
    val MARCHID  = 0x101.U(12.W)
    val SATP     = 0x180.U(12.W)
    val MSTATUS  = 0x300.U(12.W)
    val MTVEC    = 0x305.U(12.W)
    val MSCRATCH = 0x340.U(12.W)
    val MEPC     = 0x341.U(12.W)
    val MCAUSE   = 0x342.U(12.W)
}

class CSRDebugger extends BlackBox {
    val io = IO(new Bundle {
        val clk     = Input(Clock())
        val wen     = Input(Bool())
        val waddr   = Input(UInt(12.W))
        val wdata   = Input(UInt(32.W))
    })
}

class CSR extends Module {
    val io = IO(new Bundle {
        val waddr1  = Input (UInt(12.W))
        val waddr2  = Input (UInt(12.W))
        val wdata1  = Input (UInt(32.W))
        val wdata2  = Input (UInt(32.W))
        val wen1    = Input (Bool())
        val wen2    = Input (Bool())
        val raddr   = Input (UInt(12.W))
        val rdata   = Output(UInt(32.W))
        val dbg     = Output(UInt(32.W))
    })

    val mvendorid = RegInit(0x79737938.U(32.W))
    val marchid = RegInit(0.U(12.W))
    val mcause  = RegInit(0.U(32.W))
    val mepc    = RegInit(0.U(32.W))
    val mscratch= RegInit(0.U(32.W))
    val mstatus = RegInit(0x1800.U(32.W))
    val mtvec   = RegInit(0.U(32.W))
    val satp    = RegInit(0.U(32.W))

    io.dbg := mstatus

    io.rdata := MuxLookup(io.raddr, 0.U(32.W))(Seq (
        CSRAddr.MVENDORID -> mvendorid,
        CSRAddr.MARCHID -> marchid,
        CSRAddr.SATP    -> satp,
        CSRAddr.MSTATUS -> mstatus,
        CSRAddr.MTVEC   -> mtvec,
        CSRAddr.MSCRATCH-> mscratch,
        CSRAddr.MEPC    -> mepc,
        CSRAddr.MCAUSE  -> mcause
    ))

    when (io.wen1) {
        when (io.waddr1 === CSRAddr.SATP) { satp := io.wdata1 } 
        .elsewhen (io.waddr1 === CSRAddr.MSTATUS)  { mstatus  := io.wdata1 }
        .elsewhen (io.waddr1 === CSRAddr.MTVEC)    { mtvec    := io.wdata1 }
        .elsewhen (io.waddr1 === CSRAddr.MSCRATCH) { mscratch := io.wdata1 }
        .elsewhen (io.waddr1 === CSRAddr.MEPC)     { mepc     := io.wdata1 }
        .elsewhen (io.waddr1 === CSRAddr.MCAUSE)   { mcause   := io.wdata1 }
        .elsewhen (io.waddr1 === CSRAddr.MVENDORID){ mvendorid:= io.wdata1 }
        .elsewhen (io.waddr1 === CSRAddr.MARCHID)  { marchid  := io.wdata1 }
    }

    when (io.wen2) {
        when (io.waddr2 === CSRAddr.SATP) { satp := io.wdata2 } 
        .elsewhen (io.waddr2 === CSRAddr.MSTATUS)  { mstatus  := io.wdata2 }
        .elsewhen (io.waddr2 === CSRAddr.MTVEC)    { mtvec    := io.wdata2 }
        .elsewhen (io.waddr2 === CSRAddr.MSCRATCH) { mscratch := io.wdata2 }
        .elsewhen (io.waddr2 === CSRAddr.MEPC)     { mepc     := io.wdata2 }
        .elsewhen (io.waddr2 === CSRAddr.MCAUSE)   { mcause   := io.wdata2 }
        .elsewhen (io.waddr2 === CSRAddr.MVENDORID){ mvendorid:= io.wdata2 }
        .elsewhen (io.waddr2 === CSRAddr.MARCHID)  { marchid  := io.wdata2 }
    }

    val debugger = Module(new CSRDebugger())
    debugger.io.clk := clock
    debugger.io.wen := io.wen1
    debugger.io.waddr := io.waddr1
    debugger.io.wdata := io.wdata1

    val debugger2 = Module(new CSRDebugger())
    debugger2.io.clk := clock
    debugger2.io.wen := io.wen2
    debugger2.io.waddr := io.waddr2
    debugger2.io.wdata := io.wdata2
}
