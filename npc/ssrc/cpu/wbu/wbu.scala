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
        val gpr_wen   = Output(Bool())

        val dbg = new Bundle {
            val brk = Output(Bool())
            val ivd = Output(Bool())
            val done  = Output(Bool())
            val pc = Output(UInt(32.W))
            val inst = Output(UInt(32.W))
            val csr = new RegWIO(32)
        }
    })
    io.gpr_waddr := io.in.bits.gpr_waddr
    io.gpr_wdata := io.in.bits.gpr_wdata
    io.gpr_wen := io.in.bits.gpr_wen && io.in.valid

    io.in.ready := true.B
    
    // DEBUG
    io.dbg.brk  := RegEnable(io.in.bits.is_brk, io.in.valid)
    io.dbg.ivd  := RegEnable(io.in.bits.is_ivd, io.in.valid)
    io.dbg.pc   := RegEnable(io.in.bits.dbg.pc, io.in.valid)
    io.dbg.inst := RegEnable(io.in.bits.dbg.inst, io.in.valid)
    io.dbg.csr  := RegEnable(io.in.bits.dbg.csr, io.in.valid)
    io.dbg.done := RegNext(io.in.valid)
}
