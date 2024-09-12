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

        // CSR
        val csr_waddr = Output(UInt(Config.CSRAddrLength.W))
        val csr_wen   = Output(Bool())
        val csr_wdata = Output(UInt(32.W))
        
        // Data Hazard
        val gpr_waddr = Output(UInt(Config.GPRAddrLength.W))

        // Control Hazard
        val jmp = Output(Bool())
        val dnpc = Output(UInt(32.W))
    })
    val func3 = io.in.bits.func3
    val rs1 = io.in.bits.rs1
    val rs2 = io.in.bits.rs2
    val rs3 = io.in.bits.rs3
    val rs4 = io.in.bits.rs4
    io.gpr_waddr := io.in.bits.gpr_waddr
 
    val alu = Module(new Alu())
    alu.io.a := rs1
    alu.io.b := rs2
    alu.io.func3 := Mux(io.in.bits.alu_add, AluFunc3.ADD, func3)
    alu.io.tag := io.in.bits.exu_tag
    val alu_result = Mux(io.in.bits.alu_bsel, rs2, alu.io.res)

    val cmp = Module(new Cmp())
    cmp.io.a := rs3
    cmp.io.b := rs4
    cmp.io.func3 := func3

    io.out.bits.exu_result := alu_result
    val jmp = (io.in.bits.is_branch && alu.io.cmp) || io.in.bits.is_jmp
    io.jmp := jmp
    when (io.in.valid) {
        printf("0x%x %d\n", io.in.bits.dbg.pc, alu.io.cmp)
    }

    // CSR
    io.csr_waddr := io.in.bits.csr_waddr
    io.csr_wen   := io.in.bits.csr_wen
    io.csr_wdata := alu.io.csr
    io.dnpc := Mux(io.in.bits.dnpc_sel, rs2, alu_result)
    
    io.out.bits.gpr_wdata := Mux(io.in.bits.gpr_ws(0), rs1, rs3)

    // Passthrough
    io.out.bits.func3    := func3
    io.out.bits.mem_wen  := io.in.bits.mem_wen
    io.out.bits.mem_ren  := io.in.bits.mem_ren
    io.out.bits.mem_wdata:= rs4
        
    io.out.bits.gpr_waddr  := io.in.bits.gpr_waddr
    io.out.bits.gpr_wen    := io.in.bits.gpr_wen
    io.out.bits.gpr_ws     := io.in.bits.gpr_ws

    io.out.bits.is_brk := io.in.bits.is_brk
    io.out.bits.is_ivd := io.in.bits.is_ivd

    io. in.ready := io.out.ready
    io.out.valid := io.in.valid

    // DEBUG
    io.out.bits.dbg <> io.in.bits.dbg
}
