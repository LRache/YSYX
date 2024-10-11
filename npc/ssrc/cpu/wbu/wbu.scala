package cpu.wbu

import chisel3._
import chisel3.util._

import cpu.reg.GPRWSel
import cpu.LSUMessage
import cpu.reg.CSRAddr
import cpu.RegWIO

class WBU extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new LSUMessage))

        val gpr_waddr = Output(UInt(5.W))
        val gpr_wdata = Output(UInt(32.W))

        val dbg = new Bundle {
            val brk  = Output(Bool())
            val ivd  = Output(Bool())
            val done = Output(Bool())
            val pc   = Output(UInt(32.W))
            val inst = Output(UInt(32.W))
            val csr  = new RegWIO(32)
            val is_trap = Output(Bool())
            val cause = Output(UInt(32.W))
        }
    })
    io.gpr_waddr := io.in.bits.gpr_waddr
    io.gpr_wdata := io.in.bits.gpr_wdata

    io.in.ready := true.B
    
    // DEBUG
    io.dbg.brk  := io.in.bits.is_brk
    io.dbg.ivd  := io.in.bits.is_ivd
    io.dbg.pc   := io.in.bits.dbg.pc
    io.dbg.inst := io.in.bits.dbg.inst
    io.dbg.csr.waddr := io.in.bits.dbg.csr.waddr
    io.dbg.csr.wdata := io.in.bits.dbg.csr.wdata
    io.dbg.csr.wen   := io.in.bits.dbg.csr.wen && io.in.valid
    io.dbg.is_trap := io.in.bits.dbg.trap.is_trap && io.in.valid
    io.dbg.cause   := Cat(io.in.bits.dbg.trap.is_interrupt, 0.U(26.W), io.in.bits.dbg.trap.cause)
    io.dbg.done := io.in.valid
}
