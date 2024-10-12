package cpu.idu

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import cpu.Config
import cpu.reg.CSRAddr
import cpu.IFUMessage
import cpu.IDUMessage
import cpu.reg.CSR

class IDU extends Module {
    val io = IO(new Bundle {
        val in = Flipped(Decoupled(new IFUMessage))
        val out = Decoupled(new IDUMessage)

        val gpr_raddr1 = Output(UInt(Config.GPRAddrLength.W))
        val gpr_raddr2 = Output(UInt(Config.GPRAddrLength.W))
        val gpr_rdata1 = Input (UInt(32.W))
        val gpr_rdata2 = Input (UInt(32.W))
        val gpr_ren1   = Output(Bool())
        val gpr_ren2   = Output(Bool())
        val csr_raddr  = Output(UInt(Config.CSRAddrLength.W))
        val csr_rdata  = Input (UInt(32.W))
        val csr_ren    = Output(Bool())
        
        val fence_i = Output(Bool())

        val raw = Input(Bool())
        val predict_failed = Input(Bool())
    })
    val pc = io.in.bits.pc
    val snpc = io.in.bits.snpc
    val inst = io.in.bits.inst
    val op = Decoder.decode(inst)
    
    // EXU
    io.out.bits.func3 := inst(14, 12)

    io.gpr_raddr1 := io.in.bits.inst(15 + Config.GPRAddrLength - 1, 15)
    io.gpr_raddr2 := io.in.bits.inst(20 + Config.GPRAddrLength - 1, 20)
    io.gpr_ren1 := op.gprRen1
    io.gpr_ren2 := op.gprRen2
    io.csr_raddr := MuxLookup(op.csrRAddrSel, 0.U(12.W))(Seq(
          CSRAddrSel.VEC.id.U -> CSRAddr.MTVEC,
          CSRAddrSel.EPC.id.U -> CSRAddr.MEPC,
          CSRAddrSel.Ins.id.U -> CSRAddr.csr_addr_translate(inst(31, 20))
        )
    )
    io.csr_ren := op.csrRen

    def gen_one_bits(length: Int) : UInt = {
        ((1 << length) - 1).U(length.W)
    }

    val signExtend20 = Fill(20, io.in.bits.inst(31))
    val imm_i  = Cat(signExtend20, io.in.bits.inst(31, 20))
    val imm_iu = Cat(0.U(20.W), io.in.bits.inst(31, 20))
    val imm_s  = Cat(signExtend20, io.in.bits.inst(31, 25), io.in.bits.inst(11, 7))
    val imm_b  = Cat(signExtend20, io.in.bits.inst(31), io.in.bits.inst(7), io.in.bits.inst(30, 25), io.in.bits.inst(11, 8), 0.B)
    val imm_u  = Cat(io.in.bits.inst(31, 12), 0.U(12.W))
    val imm_j  = Cat(Fill(11, io.in.bits.inst(31)), io.in.bits.inst(31), io.in.bits.inst(19, 12), io.in.bits.inst(20), io.in.bits.inst(30, 21), 0.B)
    val imm_c  = Cat(0.U(27.W), io.in.bits.inst(19, 15))
    val imm = MuxLookup(op.immType, 0.U(32.W))(Seq(
            ImmType. I.id.U -> imm_i,
            ImmType.IU.id.U -> imm_iu,
            ImmType. S.id.U -> imm_s,
            ImmType. U.id.U -> imm_u,
            ImmType. B.id.U -> imm_b,
            ImmType. J.id.U -> imm_j,
            ImmType. C.id.U -> imm_c,
        )
    )
    
    val rs1 = MuxLookup(op.aSel, 0.U(32.W))(Seq(
        ASel.  PC.U -> pc,
        ASel.GPR1.U -> io.gpr_rdata1,
        ASel. CSR.U -> io.csr_rdata,
        ASel.ZERO.U -> 0.U
    ))
    val rs2 = MuxLookup(op.bSel, 0.U(32.W))(Seq(
        BSel.GPR1.U -> io.gpr_rdata1,
        BSel.GPR2.U -> io.gpr_rdata2,
        BSel. Imm.U -> imm,
        BSel. CSR.U -> io.csr_rdata
    ))
    io.out.bits.rs1 := rs1
    io.out.bits.rs2 := rs2
    io.out.bits.rs3 := Mux(op.cSel, snpc, io.gpr_rdata1)
    io.out.bits.rs4 := Mux(op.dSel, imm, io.gpr_rdata2)

    io.out.bits.exu_tag := op.exuTag
    io.out.bits.alu_add := op.aluAdd
    io.out.bits.is_branch := op.isBranch
    io.out.bits.is_jmp := op.isJmp

    // LSU
    io.out.bits.mem_wen := op.memWen
    io.out.bits.mem_ren := op.memRen

    // WBU
    // GPR
    io.out.bits.gpr_waddr := Mux(op.gprWen, inst(7 + Config.GPRAddrLength - 1, 7), 0.U)
    io.out.bits.gpr_ws := op.gprWSel
    
    // CSR
    io.out.bits.trap.is_trap := op.isIvd || op.isTrap
    io.out.bits.trap.is_interrupt := false.B
    io.out.bits.trap.cause := Mux(op.isIvd, 2.U(5.W), 11.U(5.W))
    
    io.out.bits.csr_wen := op.csrWen && io.gpr_raddr1.orR
    io.out.bits.csr_waddr := CSRAddr.csr_addr_translate(inst(31, 20))
    io.out.bits.dnpc_sel := op.dnpcSel

    // FENCE
    io.fence_i := op.fenceI && io.in.valid

    // TAG
    io.out.bits.is_ivd := op.isIvd
    io.out.bits.is_brk := op.isBrk

    io. in.ready := io.out.ready && !io.raw
    io.out.valid := io.in.valid && !io.predict_failed && !io.raw

    // DEBUG
    io.out.bits.dbg <> io.in.bits.dbg
}
