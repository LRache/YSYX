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
        
        // Data Hazard
        val gpr_waddr = Output(UInt(Config.CSRAddrLength.W))

        val jmp = Output(Bool())
        val dnpc = Output(UInt(32.W))

        val is_ecall = Output(Bool())
        val csr_wdata2 = Output(UInt(32.W))
    })
    val func3 = io.in.bits.func3
    val rs1 = io.in.bits.rs1
    val rs2 = io.in.bits.rs2
    val cmp1 = io.in.bits.cmp1
    val cmp2 = io.in.bits.cmp2
    io.gpr_waddr := io.in.bits.gpr_waddr

    val cmp = Module(new Cmp())
    cmp.io.a := cmp1
    cmp.io.b := cmp2
    cmp.io.func3 := func3
    
    val alu = Module(new Alu())
    alu.io.a := rs1
    alu.io.b := rs2
    alu.io.func3 := func3
    alu.io.tag := io.in.bits.exu_tag

    io.out.bits.exu_result := alu.io.result
    val jmp = (io.in.bits.is_branch && cmp.io.res) || io.in.bits.is_jmp
    io.jmp := jmp

    when(io.out.valid) {
        printf("%d %d\n", alu.io.a, alu.io.b)
    }

    // CSR
    io.out.bits.csr_wdata := Mux(
        io.in.bits.is_ecall,
        rs1,
        MuxLookup(func3(1,0), 0.U(32.W))(Seq (
            1.U ->         rs2,
            2.U -> (rs1 |  rs2),
            3.U -> (rs1 & ~rs2)
        ))
    )
    io.csr_wdata2 := rs1
    io.is_ecall := io.in.bits.is_ecall && io.in.valid
    io.dnpc := Mux(io.in.bits.dnpc_sel, rs2, alu.io.result)
    
    io.out.bits.gpr_wdata := Mux(io.in.bits.gpr_ws(0), rs1, io.in.bits.snpc)

    // Passthrough
    io.out.bits.func3    := func3
    io.out.bits.mem_wen  := io.in.bits.mem_wen
    io.out.bits.mem_ren  := io.in.bits.mem_ren
        
    io.out.bits.gpr_waddr  := io.in.bits.gpr_waddr
    io.out.bits.gpr_rdata2 := rs2
    io.out.bits.gpr_wen    := io.in.bits.gpr_wen
    io.out.bits.gpr_ws     := io.in.bits.gpr_ws

    io.out.bits.csr_waddr  := io.in.bits.csr_waddr
    io.out.bits.is_ecall   := io.in.bits.is_ecall

    io.out.bits.is_brk := io.in.bits.is_brk
    io.out.bits.is_ivd := io.in.bits.is_ivd

    io. in.ready := io.out.ready
    io.out.valid := io.in.valid

    // when(io.out.valid) {
    //     printf("out valid: EXU %d\n", io.out.bits.rd)
    // }
    // when(io.in.valid) {
    //     printf("EXU 0x%x %d %d\n", io.out.bits.dbg.pc, io.out.bits.gpr_waddr, io.in.bits.gpr_rdata1)
    // }
    // when(io.out.valid && io.out.ready) {
    //     printf("EXU [0x%x] %d 0x%x 0x%x 0x%x\n", io.in.bits.dbg.pc, io.out.bits.mem_ren, io.out.bits.exu_result, io.in.bits.imm, io.gpr_rdata1)
    // }

    // DEBUG
    io.out.bits.dbg.pc := io.in.bits.dbg.pc
    // assert(io.in.bits.dbg.pc =/= 0x3000000c.U)
    // when (io.in.valid) {
    //     printf("EXU 0x%x\n", io.in.bits.dbg.pc)
    // }
}
