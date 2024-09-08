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

        // val gpr_raddr1 = Output(UInt(Config.GPRAddrLength.W))
        // val gpr_raddr2 = Output(UInt(Config.GPRAddrLength.W))
        // val gpr_rdata1 = Input (UInt(32.W))
        // val gpr_rdata2 = Input (UInt(32.W))
        
        // Data Hazard
        val gpr_waddr = Output(UInt(Config.CSRAddrLength.W))

        val csr_raddr = Output(UInt(Config.CSRAddrLength.W))
        val csr_rdata = Input (UInt(32.W))

        val jmp = Output(Bool())
        val dnpc = Output(UInt(32.W))
        // val predict_failed = Input(Bool())

        val is_ecall = Output(Bool())
        val csr_wdata2 = Output(UInt(32.W))
    })
    // io.gpr_raddr1 := io.in.bits.gpr_raddr1
    // io.gpr_raddr2 := io.in.bits.gpr_raddr2
    val gpr_rdata1 = io.in.bits.gpr_rdata1
    val gpr_rdata2 = io.in.bits.gpr_rdata2
    io.csr_raddr := io.in.bits.csr_raddr
    io.gpr_waddr := io.in.bits.gpr_waddr

    val cmp = Module(new Cmp())
    cmp.io.a    := gpr_rdata1
    cmp.io.b    := gpr_rdata2
    cmp.io.sel  := io.in.bits.cmp_sel
    
    val alu = Module(new Alu())
    alu.io.a    := Mux(io.in.bits.a_sel, io.in.bits.pc , gpr_rdata1)
    alu.io.b    := Mux(io.in.bits.b_sel, io.in.bits.imm, gpr_rdata2)
    alu.io.sel  := io.in.bits.alu_sel

    // io.out.bits.pc_sel := io.in.bits.is_jmp && cmp.io.res
    // io.out.bits.pc_sel := cmp.io.res
    io.out.bits.exu_result := alu.io.result
    io.jmp := cmp.io.res

    // CSR
    io.out.bits.csr_wdata := Mux(
        io.in.bits.csr_wd_sel,
        io.in.bits.pc,
        MuxLookup(io.in.bits.csr_ws, 0.U(32.W))(Seq (
            CSRWSel. W.id.U -> gpr_rdata1,
            CSRWSel. S.id.U -> (io.csr_rdata |   gpr_rdata1 ),
            CSRWSel. C.id.U -> (io.csr_rdata & (~gpr_rdata1)),
            CSRWSel.WI.id.U -> io.in.bits.imm,
            CSRWSel.SI.id.U -> (io.csr_rdata |   io.in.bits.imm ),
            CSRWSel.CI.id.U -> (io.csr_rdata & (~io.in.bits.imm)),
        ))
    )
    // io.out.bits.csr_wdata2 := io.in.bits.rs2
    // io.csr_wdata2 := io.in.bits.rs2
    io.csr_wdata2 := gpr_rdata2
    io.is_ecall := io.in.bits.is_ecall && io.in.valid
    // io.out.bits.dnpc := Mux(io.in.bits.dnpc_sel, io.in.bits.csr_rdata, alu.io.result)
    io.dnpc := Mux(io.in.bits.dnpc_sel, io.csr_rdata, alu.io.result)
    
    io.out.bits.gpr_wdata := Mux(io.in.bits.gpr_ws(0), io.csr_rdata, io.in.bits.snpc)

    // Passthrough
    io.out.bits.mem_wen  := io.in.bits.mem_wen
    io.out.bits.mem_ren  := io.in.bits.mem_ren
    io.out.bits.mem_type := io.in.bits.mem_type
        
    io.out.bits.gpr_waddr  := io.in.bits.gpr_waddr
    io.out.bits.gpr_rdata2 := gpr_rdata2
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
    when(io.in.valid) {
        printf("EXU 0x%x %d %d\n", io.out.bits.dbg.pc, io.out.bits.gpr_waddr, io.in.bits.gpr_rdata1)
    }
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
