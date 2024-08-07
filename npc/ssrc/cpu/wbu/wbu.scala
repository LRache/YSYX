package cpu.wbu

import chisel3._
import chisel3.util._

import cpu.reg.RegWSel
import cpu.LSUMessage
import cpu.WBUMessage

class WBU extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new LSUMessage))
        val out = Decoupled(new WBUMessage)

        val reg_waddr = Output(UInt(5.W))
        val reg_wdata = Output(UInt(32.W))
        val reg_wen   = Output(Bool())

        val csr_waddr1 = Output(UInt(12.W))
        val csr_waddr2 = Output(UInt(12.W))
        val csr_wdata1 = Output(UInt(32.W))
        val csr_wdata2 = Output(UInt(32.W))
        val csr_wen1   = Output(Bool())
        val csr_wen2   = Output(Bool())

        val is_brk = Output(Bool())
        val is_inv = Output(Bool())

        val dbg = Output(UInt(32.W))
    })
    io.reg_waddr := io.in.bits.rd
    io.reg_wdata := MuxLookup(io.in.bits.reg_ws, 0.U)(Seq (
        RegWSel.DIS.id.U -> 0.U,
        RegWSel.EXU.id.U -> io.in.bits.exu_result,
        RegWSel. SN.id.U -> io.in.bits.snpc,
        RegWSel.MEM.id.U -> io.in.bits.mem_rdata,
        RegWSel.CSR.id.U -> io.in.bits.csr_rdata
    ))
    io.reg_wen := io.in.bits.reg_wen & io.in.valid

    io.csr_waddr1 := io.in.bits.csr_waddr1
    io.csr_waddr2 := io.in.bits.csr_waddr2
    io.csr_wdata1 := Mux(io.in.bits.csr_wd_sel, io.in.bits.pc, io.in.bits.csr_wdata1)
    io.csr_wdata2 := io.in.bits.csr_wdata2
    io.csr_wen1   := io.in.bits.csr_wen1 & io.in.valid
    io.csr_wen2   := io.in.bits.csr_wen2 & io.in.valid
    io.is_brk := io.in.bits.is_brk & io.in.valid
    io.is_inv := io.in.bits.is_ivd & io.in.valid

    io.dbg := io.in.bits.csr_wdata2

    io.out.bits.dnpc := io.in.bits.dnpc
    io.out.bits.pc_sel := io.in.bits.pc_sel

    io. in.ready := true.B
    io.out.valid := io.in.valid
}
