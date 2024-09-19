package cpu.reg

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import cpu.Config.CSRAddrLength
import cpu.Config
import chisel3.util.RegEnable
import cpu.RegWIO

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

    def csr_addr_translate(origin: UInt): UInt = {
        // val truthTable = TruthTable(Map(
        //     BitPat(0x100.U(12.W)) -> BitPat(CSRAddr.MVENDORID),
        //     BitPat(0x101.U(12.W)) -> BitPat(CSRAddr.MARCHID),
        //     // BitPat(0x180.U(12.W)) -> BitPat(CSRAddr.SATP),
        //     BitPat(0x300.U(12.W)) -> BitPat(CSRAddr.MSTATUS),
        //     BitPat(0x305.U(12.W)) -> BitPat(CSRAddr.MTVEC),
        //     BitPat(0x340.U(12.W)) -> BitPat(CSRAddr.MSCRATCH),
        //     BitPat(0x341.U(12.W)) -> BitPat(CSRAddr.MEPC),
        //     BitPat(0x342.U(12.W)) -> BitPat(CSRAddr.MCAUSE) 
        // ), BitPat(0.U(4.W)))
        // decoder(origin, truthTable)
        MuxLookup(origin, 0.U(Config.CSRAddrLength.W)) (Seq(
            0x100.U(12.W) -> CSRAddr.MVENDORID,
            0x101.U(12.W) -> CSRAddr.MARCHID,
            // 0x180.U(12.W) -> CSRAddr.SATP,
            0x300.U(12.W) -> CSRAddr.MSTATUS,
            0x305.U(12.W) -> CSRAddr.MTVEC,
            0x340.U(12.W) -> CSRAddr.MSCRATCH,
            0x341.U(12.W) -> CSRAddr.MEPC,
            0x342.U(12.W) -> CSRAddr.MCAUSE
        ))
    }
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
        val w       = Flipped(new RegWIO(CSRAddrLength))
        val cause_en= Input (Bool())
        val cause   = Input (UInt(32.W))
        val raddr   = Input (UInt(4.W))
        val rdata   = Output(UInt(32.W))
    })

    def gen_csr(addr : UInt, name : String) : UInt = {
        RegEnable(io.w.wdata, Config.CSRInitValue(name).U(32.W), io.w.wen && io.w.waddr === addr)
    }

    val mcause  = RegEnable(Mux(io.cause_en, io.cause, io.w.wdata) , 0.U(32.W), (io.w.waddr === CSRAddr.MCAUSE && io.w.wen) || io.cause_en)
    
    val mepc    = gen_csr(CSRAddr.MEPC,     "mepc"      )
    val mscratch= gen_csr(CSRAddr.MSCRATCH, "mscratch"  )
    val mstatus = gen_csr(CSRAddr.MSTATUS,  "mstatus"   )
    val mtvec   = gen_csr(CSRAddr.MTVEC,    "mtvec"     )
    // val satp    = gen_csr(CSRAddr.SATP,     "satp"      )

    io.rdata := MuxLookup(io.raddr, 0.U(32.W))(Seq (
        CSRAddr.MVENDORID -> Config.VendorID.U(32.W),
        CSRAddr.MARCHID -> Config.ArchID.U(32.W),
        // CSRAddr.SATP    -> satp,
        CSRAddr.MSTATUS -> mstatus,
        CSRAddr.MTVEC   -> mtvec,
        CSRAddr.MSCRATCH-> mscratch,
        CSRAddr.MEPC    -> mepc,
        CSRAddr.MCAUSE  -> mcause
    ))
}
