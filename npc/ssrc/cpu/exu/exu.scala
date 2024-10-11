package cpu.exu

import chisel3._
import chisel3.util._

import cpu.reg.CSRWSel
import cpu.IDUMessage
import cpu.EXUMessage
import cpu.Config
import cpu.RegWIO
import cpu.reg.GPRWSel
import cpu.TrapMessage

class EXU extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new IDUMessage))
        val out = Decoupled(new EXUMessage)

        // CSR
        val csr = new RegWIO(Config.CSRAddrLength)
        
        // Trap
        val trap = new TrapMessage
        val epc = Output(UInt(32.W))

        // Data Hazard
        val gprWSel = Output(Bool())

        // Control Hazard
        val jmp = Output(Bool())
        val dnpc = Output(UInt(32.W))
    })
    val func3 = io.in.bits.func3
    val rs1 = io.in.bits.rs1
    val rs2 = io.in.bits.rs2
    val rs3 = io.in.bits.rs3
    val rs4 = io.in.bits.rs4
 
    val alu = Module(new Alu())
    alu.io.a := rs1
    alu.io.b := rs2
    alu.io.c := rs3
    alu.io.d := rs4
    alu.io.func3 := func3
    alu.io.addT  := io.in.bits.alu_add
    alu.io.tag   := io.in.bits.exu_tag
    val alu_result = alu.io.res

    io.out.bits.rs := MuxLookup(io.in.bits.gpr_ws, 0.U)(Seq (
        GPRWSel.SNPC.U -> rs3,
        GPRWSel. CSR.U -> rs1,
        GPRWSel. EXU.U -> alu_result,
        GPRWSel. MEM.U -> alu_result,
    ))
    io.gprWSel := io.in.bits.gpr_ws === GPRWSel.MEM.U
    
    val jmp = (io.in.bits.is_branch && alu.io.cmp) || io.in.bits.is_jmp || io.in.bits.trap.is_trap
    io.jmp := jmp
    io.dnpc := Mux(io.in.bits.dnpc_sel, rs2, alu_result)
    
    // Trap
    io.trap := io.in.bits.trap
    io.epc := rs1
    
    // CSR
    io.csr.waddr := io.in.bits.csr_waddr
    io.csr.wdata := alu.io.csr
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
    io.out.valid := io.in.valid

    // DEBUG
    io.out.bits.dbg.pc := io.in.bits.dbg.pc
    io.out.bits.dbg.inst := io.in.bits.dbg.inst
    io.out.bits.dbg.csr.waddr := io.csr.waddr
    io.out.bits.dbg.csr.wdata := io.csr.wdata
    io.out.bits.dbg.csr.wen   := io.csr.wen
    io.out.bits.dbg.trap := io.in.bits.trap
}