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
        // val out = Decoupled(new WBUMessage)

        val gpr_waddr = Output(UInt(5.W))
        val gpr_wdata = Output(UInt(32.W))
        val gpr_wen   = Output(Bool())

        val csr_waddr1 = Output(UInt(12.W))
        val is_ecall = Output(Bool())
        val csr_wdata1 = Output(UInt(32.W))

        val is_brk = Output(Bool())
        val is_inv = Output(Bool())
        val valid  = Output(Bool())
        val dbg_pc = Output(UInt(32.W))
    })
    io.gpr_waddr := io.in.bits.gpr_waddr
    io.gpr_wdata := io.in.bits.gpr_wdata
    io.gpr_wen := io.in.bits.gpr_wen && io.in.valid

    io.csr_waddr1 := Mux(io.in.valid, io.in.bits.csr_waddr, CSRAddr.NONE)
    io.csr_wdata1 := io.in.bits.csr_wdata
    io.is_ecall := io.in.bits.is_ecall && io.in.valid
    
    io.is_brk := RegNext(io.in.bits.is_brk && io.in.valid, false.B)
    io.is_inv := RegNext(io.in.bits.is_ivd && io.in.valid, false.B)
    io.valid  := RegNext(io.in.valid)
    // io.is_brk := io.in.bits.is_brk && io.in.valid
    // io.is_inv := io.in.bits.is_ivd && io.in.valid
    
    // io.out.bits.is_brk := io.in.bits.is_brk
    // io.out.bits.is_ivd := io.in.bits.is_ivd
    // io.out.bits.dnpc := io.in.bits.dnpc
    // io.out.bits.pc_sel := io.in.bits.pc_sel

    io. in.ready := true.B
    // io.out.valid := io.in.valid

    // when(io.in.valid) {
    //     printf("in valid WBU %x %d\n", io.in.bits.gpr_wdata, io.in.bits.gpr_waddr)
    // }

    // DEBUG
    io.dbg_pc := RegEnable(io.in.bits.dbg.pc, io.in.valid)
    // io.out.bits.dbg.pc := io.in.bits.dbg.pc
    // when (io.in.valid) {
    //     printf("WBU 0x%x\n", io.in.bits.dbg.pc)
    // }
}
