package cpu.wbu

import chisel3._
import chisel3.util._

import cpu.reg.GPRWSel
import cpu.LSUMessage
import cpu.WBUMessage
import cpu.reg.CSRAddr

class WBU extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new LSUMessage))
        val out = Decoupled(new WBUMessage)

        val reg_waddr = Output(UInt(5.W))
        val reg_wdata = Output(UInt(32.W))
        val reg_wen   = Output(Bool())

        val csr_waddr1 = Output(UInt(12.W))
        val is_ecall = Output(Bool())
        // val csr_waddr2 = Output(UInt(12.W))
        val csr_wdata1 = Output(UInt(32.W))
        // val csr_wdata2 = Output(UInt(32.W))
        val csr_wen1   = Output(Bool())
        // val csr_wen2   = Output(Bool())

        val is_brk = Output(Bool())
        val is_inv = Output(Bool())
    })
    io.reg_waddr := io.in.bits.rd
    io.reg_wdata := io.in.bits.gpr_wdata
    io.reg_wen := io.in.bits.gpr_wen && io.in.valid

    io.csr_waddr1 := Mux(io.in.valid, io.in.bits.csr_waddr1, CSRAddr.NONE)
    io.csr_wdata1 := io.csr_wdata1
    io.csr_wen1 := true.B
    io.is_ecall := io.in.bits.is_ecall && io.in.valid
    io.is_brk := io.in.bits.is_brk && io.in.valid
    io.is_inv := io.in.bits.is_ivd && io.in.valid

    io.out.bits.dnpc := io.in.bits.dnpc
    io.out.bits.pc_sel := io.in.bits.pc_sel

    io. in.ready := true.B
    io.out.valid := io.in.valid
}
