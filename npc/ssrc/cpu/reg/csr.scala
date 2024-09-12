package cpu.reg

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.MuxCase

import cpu.Config
import chisel3.util.RegEnable

object CSRWSel extends Enumeration {
    type CSRWSel = Value
    val W, S, C, WI, SI, CI = Value
}

object CSRAddr {
    val MVENDORID   = 0x0.U(4.W)
    val MARCHID     = 0x1.U(4.W)
    val SATP        = 0x2.U(4.W)
    val MSTATUS     = 0x3.U(4.W)
    val MTVEC       = 0x4.U(4.W)
    val MSCRATCH    = 0x5.U(4.W)
    val MEPC        = 0x6.U(4.W)
    val MCAUSE      = 0x7.U(4.W)
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
        val waddr   = Input (UInt(Config.CSRAddrLength.W))
        val wen     = Input (Bool())
        val wdata   = Input (UInt(32.W))
        val cause_en= Input (Bool())
        val cause   = Input (UInt(32.W))
        val raddr   = Input (UInt(4.W))
        val rdata   = Output(UInt(32.W))
    })

    val mcause  = RegEnable(Mux(io.cause_en, io.cause, io.wdata) , 0.U(32.W), (io.waddr === CSRAddr.MCAUSE && io.wen) || io.cause_en)
    val mepc    = RegEnable(io.wdata, 0.U(32.W),      io.wen && io.waddr === CSRAddr.MEPC    )
    val mscratch= RegEnable(io.wdata, 0.U(32.W),      io.wen && io.waddr === CSRAddr.MSCRATCH)
    val mstatus = RegEnable(io.wdata, 0x1800.U(32.W), io.wen && io.waddr === CSRAddr.MSTATUS )
    val mtvec   = RegEnable(io.wdata, 0.U(32.W),      io.wen && io.waddr === CSRAddr.MTVEC   )
    val satp    = RegEnable(io.wdata, 0.U(32.W),      io.wen && io.waddr === CSRAddr.SATP    )

    io.rdata := MuxLookup(io.raddr, 0.U(32.W))(Seq (
        CSRAddr.MVENDORID -> Config.VendorID.U(32.W),
        CSRAddr.MARCHID -> Config.ArchID.U(32.W),
        CSRAddr.SATP    -> satp,
        CSRAddr.MSTATUS -> mstatus,
        CSRAddr.MTVEC   -> mtvec,
        CSRAddr.MSCRATCH-> mscratch,
        CSRAddr.MEPC    -> mepc,
        CSRAddr.MCAUSE  -> mcause
    ))

    if (Config.HasDBG) {
        val debugger = Module(new CSRDebugger())
        debugger.io.clk := clock
        debugger.io.wen := io.wen
        debugger.io.waddr := io.waddr
        debugger.io.wdata := io.wdata

        val debugger2 = Module(new CSRDebugger())
        debugger2.io.clk := clock
        debugger2.io.wen := io.cause_en
        debugger2.io.waddr := CSRAddr.MCAUSE
        debugger2.io.wdata := io.cause
    }
}
