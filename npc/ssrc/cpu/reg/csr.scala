package cpu.reg

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import cpu.Config
import chisel3.util.RegEnable
import cpu.RegWIO
import scala.collection.mutable.ArrayBuffer
import cpu.TrapMessage

object CSRWSel extends Enumeration {
    type CSRWSel = Value
    val W, S, C, WI, SI, CI = Value
}

object CSRAddr {
    val MVENDORID   = 0x0.U(Config.CSRAddrLength.W)
    val MARCHID     = 0x1.U(Config.CSRAddrLength.W)
    val SATP        = 0x2.U(Config.CSRAddrLength.W)
    val MSTATUS     = 0x3.U(Config.CSRAddrLength.W)
    val MTVEC       = 0x4.U(Config.CSRAddrLength.W)
    val MSCRATCH    = 0x5.U(Config.CSRAddrLength.W)
    val MEPC        = 0x6.U(Config.CSRAddrLength.W)
    val MCAUSE      = 0x7.U(Config.CSRAddrLength.W)

    def csr_addr_translate(origin: UInt): UInt = {
        val table = ArrayBuffer(
            0x100.U(12.W) -> CSRAddr.MVENDORID,
            0x101.U(12.W) -> CSRAddr.MARCHID,
            0x300.U(12.W) -> CSRAddr.MSTATUS,
            0x305.U(12.W) -> CSRAddr.MTVEC,
            0x341.U(12.W) -> CSRAddr.MEPC,
            0x342.U(12.W) -> CSRAddr.MCAUSE
        )
        if (Config.HasMscratch) { table.append(0x340.U(12.W) -> CSRAddr.MSCRATCH) }
        if (Config.HasSatp    ) { table.append(0x180.U(12.W) -> CSRAddr.SATP    ) }
        return MuxLookup(origin, 0.U(Config.CSRAddrLength.W))(table.toSeq)
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
        val w       = Flipped(new RegWIO(Config.CSRAddrLength))
        val raddr   = Input (UInt(Config.CSRAddrLength.W))
        val rdata   = Output(UInt(32.W))

        val trap = Flipped(new TrapMessage)
    })

    def gen_csr(addr : UInt, name : String, init: Boolean) : UInt = {
        if (init) {
            return RegEnable(io.w.wdata, Config.CSRInitValue(name).U(32.W), io.w.wen && io.w.waddr === addr)
        } else {
            return RegEnable(io.w.wdata, io.w.wen && io.w.waddr === addr)
        }
    }

    val cause = Cat(io.trap.is_interrupt, 0.U(27.W), io.trap.cause)
    val mcause = RegInit(0.U(32.W))
    mcause := Mux(
        io.trap.is_trap, 
        cause, 
        Mux(
            io.w.waddr === CSRAddr.MCAUSE && io.w.wen, 
            io.w.wdata, mcause
        )
    )

    val mepc = RegEnable(
        io.w.wdata, 
        io.trap.is_trap || (io.w.wen && io.w.waddr === CSRAddr.MEPC)
    )

    val mstatus = gen_csr(CSRAddr.MSTATUS,  "mstatus", true)
    val mtvec   = gen_csr(CSRAddr.MTVEC,    "mtvec"  , false)
    
    val mscratch= Wire(UInt(32.W))
    val satp    = Wire(UInt(32.W))
    if (Config.HasMscratch) { mscratch := gen_csr(CSRAddr.MSCRATCH, "mscratch", false) } else { mscratch := 0.U }
    if (Config.HasSatp    ) { satp     := gen_csr(CSRAddr.SATP    , "satp"    , false) } else { satp     := 0.U }

    val table = ArrayBuffer(
        CSRAddr.MVENDORID -> Config.VendorID.U(32.W),
        CSRAddr.MARCHID -> Config.ArchID.U(32.W),
        CSRAddr.MSTATUS -> mstatus, 
        CSRAddr.MTVEC   -> mtvec,
        CSRAddr.MEPC    -> mepc,
        CSRAddr.MCAUSE  -> mcause
    )
    if (Config.HasMscratch) { table.append(CSRAddr.MSCRATCH-> mscratch) }
    if (Config.HasSatp    ) { table.append(CSRAddr.SATP    -> satp    ) }

    io.rdata := MuxLookup(io.raddr, 0.U(32.W))(table.toSeq)
}
