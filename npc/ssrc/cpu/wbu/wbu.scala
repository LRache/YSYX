package cpu.wbu

import chisel3._
import chisel3.util._

import cpu.reg.GPRWSel
import cpu.LSUMessage
import cpu.reg.CSRAddr

class WBU extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new LSUMessage))

        val gpr_waddr = Output(UInt(5.W))
        val gpr_wdata = Output(UInt(32.W))
        val gpr_wen   = Output(Bool())

        val csr_waddr1 = Output(UInt(12.W))
        val is_ecall = Output(Bool())
        val csr_wdata1 = Output(UInt(32.W))

        val dbg = new Bundle {
            val brk = Output(Bool())
            val inv = Output(Bool())
            val done  = Output(Bool())
            val pc = Output(UInt(32.W))
            val inst = Output(UInt(32.W))
        }
    })
    io.gpr_waddr := io.in.bits.gpr_waddr
    io.gpr_wdata := io.in.bits.gpr_wdata
    io.gpr_wen := io.in.bits.gpr_wen && io.in.valid

    io.csr_waddr1 := Mux(io.in.valid, io.in.bits.csr_waddr, CSRAddr.NONE)
    io.csr_wdata1 := io.in.bits.csr_wdata
    io.is_ecall := io.in.bits.is_ecall && io.in.valid
    io.in.ready := true.B
    
    // DEBUG
    io.dbg.brk  := io.in.bits.is_brk
    io.dbg.inv  := io.in.bits.is_ivd
    io.dbg.pc   := io.in.bits.dbg.pc
    io.dbg.inst := io.in.bits.dbg.inst
    io.dbg.done := io.in.valid
}
