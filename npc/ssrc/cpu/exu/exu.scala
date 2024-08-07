package cpu.exu

import chisel3._
import chisel3.util._

import cpu.reg.CSRWSel
import cpu.IDUMessage
import cpu.EXUMessage

class EXU extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new IDUMessage))
        val out = Decoupled(new EXUMessage)
        
        val dbg     = Output(UInt(32.W))
    })
    val cmp = Module(new Cmp())
    cmp.io.a    := io.in.bits.rs1
    cmp.io.b    := io.in.bits.rs2
    cmp.io.sel  := io.in.bits.cmp_sel
    
    val alu = Module(new Alu())
    alu.io.a    := Mux(io.in.bits.a_sel, io.in.bits.pc , io.in.bits.rs1)
    alu.io.b    := Mux(io.in.bits.b_sel, io.in.bits.imm, io.in.bits.rs2)
    alu.io.sel  := io.in.bits.alu_sel

    io.out.bits.pc_sel := io.in.bits.is_jmp && cmp.io.res
    io.out.bits.exu_result := alu.io.result

    // CSR
    io.out.bits.csr_wdata1 := MuxLookup(io.in.bits.csr_ws, 0.U(32.W))(Seq (
        CSRWSel. W.id.U -> io.in.bits.rs1,
        CSRWSel. S.id.U -> (io.in.bits.csr_rdata |   io.in.bits.rs1 ),
        CSRWSel. C.id.U -> (io.in.bits.csr_rdata & (~io.in.bits.rs1)),
        CSRWSel.WI.id.U -> io.in.bits.csr_imm,
        CSRWSel.SI.id.U -> (io.in.bits.csr_rdata |   io.in.bits.csr_imm ),
        CSRWSel.CI.id.U -> (io.in.bits.csr_rdata & (~io.in.bits.csr_imm)),
    )) 
    io.out.bits.csr_wdata2 := io.in.bits.rs2

    io.dbg := io.in.bits.csr_ws

    // Passthrough
    io.out.bits.mem_wen  := io.in.bits.mem_wen
    io.out.bits.mem_ren  := io.in.bits.mem_ren
    io.out.bits.mem_type := io.in.bits.mem_type
        
    io.out.bits.rd      := io.in.bits.rd
    io.out.bits.rs2     := io.in.bits.rs2
    io.out.bits.reg_wen := io.in.bits.reg_wen
    io.out.bits.reg_ws  := io.in.bits.reg_ws

    io.out.bits.csr_waddr1 := io.in.bits.csr_waddr1
    io.out.bits.csr_waddr2 := io.in.bits.csr_waddr2
    io.out.bits.csr_wen1   := io.in.bits.csr_wen1
    io.out.bits.csr_wen2   := io.in.bits.csr_wen2
    io.out.bits.csr_wd_sel := io.in.bits.csr_wd_sel
    io.out.bits.csr_ws     := io.in.bits.csr_ws
    io.out.bits.csr_imm    := io.in.bits.csr_imm
    io.out.bits.csr_rdata  := io.in.bits.csr_rdata
    io.out.bits.snpc       := io.in.bits.snpc
    io.out.bits.pc         := io.in.bits.pc
    
    io.out.bits.dnpc := Mux(io.in.bits.dnpc_sel, io.in.bits.csr_rdata, alu.io.result)

    io.out.bits.is_brk := io.in.bits.is_brk
    io.out.bits.is_ivd := io.in.bits.is_ivd

    io. in.ready := io.out.ready
    io.out.valid := io. in.valid
}
