package cpu.exu

import chisel3._
import chisel3.util._

import cpu.idu.GPRWSel
import cpu.reg.CSRWSel
import cpu.IDUMessage
import cpu.EXUMessage
import cpu.Config
import cpu.RegWIO
import cpu.TrapMessage

class EXU extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new IDUMessage))
        val out = Decoupled(new EXUMessage)

        // CSR
        val csr = new RegWIO(Config.CSRAddrWidth)
        
        // Trap
        val trap = new TrapMessage

        // Data Hazard
        val gprWSel = Output(Bool())

        // Control Hazard
        val jmp = Output(Bool())
        val predict_jmp = Output(Bool())
        val is_branch = Output(Bool())
        val dnpc = Output(UInt(30.W))
        val predictor_pc = Output(UInt(30.W))
    })
    val func3 = io.in.bits.func3
    val rs1 = io.in.bits.rs1
    val rs2 = io.in.bits.rs2
    val rs3 = io.in.bits.rs3
    val rs4 = io.in.bits.rs4

    val alu_res = Wire(UInt(32.W))
    val alu_cmp = Wire(Bool())
    val alu_csr = Wire(UInt(32.W))

    AluInline(
        a = io.in.bits.rs1, 
        b = io.in.bits.rs2, 
        c = io.in.bits.rs3, 
        d = io.in.bits.rs4, 
        func3 = io.in.bits.func3, 
        addT  = io.in.bits.alu_add, 
        tag   = io.in.bits.exu_tag, 
        res   = alu_res, 
        cmp   = alu_cmp, 
        csr   = alu_csr
    )

    val gpr_ws = io.in.bits.gpr_ws
    io.out.bits.rs := MuxLookup(gpr_ws, 0.U)(Seq (
        GPRWSel.SNPC.U -> rs3,
        GPRWSel. CSR.U -> rs1,
        GPRWSel. EXU.U -> alu_res,
        GPRWSel. MEM.U -> alu_res,
    ))
    io.gprWSel := io.in.bits.mem_ren
    
    val jmp = (io.in.bits.is_branch && alu_cmp) || io.in.bits.is_jmp || io.in.bits.trap.is_trap
    io.jmp := jmp
    io.predict_jmp := io.in.bits.predict_jmp
    io.is_branch := io.in.bits.is_branch
    io.dnpc := Mux(io.in.bits.dnpc_sel || io.in.bits.trap.is_trap, rs2, alu_res)(31, 2)
    io.predictor_pc := io.in.bits.predictor_pc(31, 2)
    
    // Trap
    io.trap := io.in.bits.trap
    
    // CSR
    io.csr.waddr := io.in.bits.csr_waddr
    io.csr.wdata := Mux(io.in.bits.trap.is_trap, rs1, alu_csr)
    io.csr.wen   := io.in.bits.csr_wen && io.in.valid

    // Passthrough
    io.out.bits.func3    := func3
    io.out.bits.mem_wen  := io.in.bits.mem_wen
    io.out.bits.mem_ren  := io.in.bits.mem_ren
    io.out.bits.mem_wdata:= rs4
        
    io.out.bits.gpr_waddr  := io.in.bits.gpr_waddr

    io.out.bits.is_brk := io.in.bits.is_brk
    io.out.bits.is_ivd := io.in.bits.is_ivd

    io. in.ready := io.out.ready
    io.out.valid := io. in.valid

    // DEBUG
    io.out.bits.dbg.pc := io.in.bits.dbg.pc
    io.out.bits.dbg.inst := io.in.bits.dbg.inst
    io.out.bits.dbg.csr.waddr := io.csr.waddr
    io.out.bits.dbg.csr.wdata := io.csr.wdata
    io.out.bits.dbg.csr.wen   := io.csr.wen
    io.out.bits.dbg.trap := io.in.bits.trap
}