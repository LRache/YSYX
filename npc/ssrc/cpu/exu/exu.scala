package cpu.exu

import chisel3._
import chisel3.util._

import cpu.reg.CSRWSel
import cpu.IDUMessage
import cpu.EXUMessage
import cpu.Config

class EXU extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new IDUMessage))
        val out = Decoupled(new EXUMessage)

        val gpr_raddr1 = Output(UInt(Config.GPRAddrLength.W))
        val gpr_raddr2 = Output(UInt(Config.GPRAddrLength.W))
        val gpr_rdata1 = Input (UInt(32.W))
        val gpr_rdata2 = Input (UInt(32.W))

        val is_ecall = Output(Bool())
        val csr_wdata2 = Output(UInt(32.W))
    })
    io.gpr_raddr1 := io.in.bits.gpr_raddr1
    io.gpr_raddr2 := io.in.bits.gpr_raddr2

    val cmp = Module(new Cmp())
    // cmp.io.a    := io.in.bits.rs1
    // cmp.io.b    := io.in.bits.rs2
    cmp.io.a    := io.gpr_rdata1
    cmp.io.b    := io.gpr_rdata2
    cmp.io.sel  := io.in.bits.cmp_sel
    
    val alu = Module(new Alu())
    // alu.io.a    := Mux(io.in.bits.a_sel, io.in.bits.pc , io.in.bits.rs1)
    // alu.io.b    := Mux(io.in.bits.b_sel, io.in.bits.imm, io.in.bits.rs2)
    alu.io.a    := Mux(io.in.bits.a_sel, io.in.bits.pc , io.gpr_rdata1)
    alu.io.b    := Mux(io.in.bits.b_sel, io.in.bits.imm, io.gpr_rdata2)
    alu.io.sel  := io.in.bits.alu_sel

    // io.out.bits.pc_sel := io.in.bits.is_jmp && cmp.io.res
    io.out.bits.pc_sel := cmp.io.res
    io.out.bits.exu_result := alu.io.result

    // CSR
    io.out.bits.csr_wdata1 := Mux(
        io.in.bits.csr_wd_sel,
        io.in.bits.pc,
        MuxLookup(io.in.bits.csr_ws, 0.U(32.W))(Seq (
            // CSRWSel. W.id.U -> io.in.bits.rs1,
            // CSRWSel. S.id.U -> (io.in.bits.csr_rdata |   io.in.bits.rs1 ),
            // CSRWSel. C.id.U -> (io.in.bits.csr_rdata & (~io.in.bits.rs1)),
            CSRWSel. W.id.U -> io.gpr_rdata1,
            CSRWSel. S.id.U -> (io.in.bits.csr_rdata |   io.gpr_rdata1 ),
            CSRWSel. C.id.U -> (io.in.bits.csr_rdata & (~io.gpr_rdata1)),
            CSRWSel.WI.id.U -> io.in.bits.imm,
            CSRWSel.SI.id.U -> (io.in.bits.csr_rdata |   io.in.bits.imm ),
            CSRWSel.CI.id.U -> (io.in.bits.csr_rdata & (~io.in.bits.imm)),
        ))
    )
    // io.out.bits.csr_wdata2 := io.in.bits.rs2
    // io.csr_wdata2 := io.in.bits.rs2
    io.csr_wdata2 := io.gpr_rdata2
    io.is_ecall := io.in.bits.is_ecall && io.in.valid
    io.out.bits.dnpc := Mux(io.in.bits.dnpc_sel, io.in.bits.csr_rdata, alu.io.result)
    io.out.bits.gpr_wdata := Mux(io.in.bits.gpr_ws(0).asBool, io.in.bits.csr_rdata, io.in.bits.snpc)

    // Passthrough
    io.out.bits.mem_wen  := io.in.bits.mem_wen
    io.out.bits.mem_ren  := io.in.bits.mem_ren
    io.out.bits.mem_type := io.in.bits.mem_type
        
    io.out.bits.rd      := io.in.bits.rd
    io.out.bits.rs2     := io.gpr_rdata2
    io.out.bits.gpr_wen := io.in.bits.gpr_wen
    io.out.bits.gpr_ws  := io.in.bits.gpr_ws

    io.out.bits.csr_waddr1 := io.in.bits.csr_waddr1
    // io.out.bits.csr_waddr2 := io.in.bits.csr_waddr2
    io.out.bits.is_ecall   := io.in.bits.is_ecall
    // io.out.bits.csr_wen1   := io.in.bits.csr_wen1
    // io.out.bits.csr_wen2   := io.in.bits.csr_wen2
    // io.out.bits.csr_ws     := io.in.bits.csr_ws
    // io.out.bits.pc         := io.in.bits.pc

    io.out.bits.is_brk := io.in.bits.is_brk
    io.out.bits.is_ivd := io.in.bits.is_ivd

    io. in.ready := io.out.ready
    io.out.valid := io. in.valid
}
