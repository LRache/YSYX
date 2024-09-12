package cpu.idu

import chisel3._
import chisel3.util._

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
    def csr_addr_translate(origin: UInt): UInt = {
        MuxLookup(origin, 0.U(Config.CSRAddrLength.W)) (Seq(
            0x100.U(12.W) -> CSRAddr.MVENDORID,
            0x101.U(12.W) -> CSRAddr.MARCHID,
            0x180.U(12.W) -> CSRAddr.SATP,
            0x300.U(12.W) -> CSRAddr.MSTATUS,
            0x305.U(12.W) -> CSRAddr.MTVEC,
            0x340.U(12.W) -> CSRAddr.MSCRATCH,
            0x341.U(12.W) -> CSRAddr.MEPC,
            0x342.U(12.W) -> CSRAddr.MCAUSE
        ))
    }

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
          CSRAddrSel.Ins.id.U -> csr_addr_translate(inst(31, 20))
        )
    )
    io.csr_ren := op.csrRen

    val imm_i  = Cat(Fill(20, io.in.bits.inst(31)), io.in.bits.inst(31, 20))
    val imm_iu = Cat(0.U(20.W), io.in.bits.inst(31, 20))
    val imm_s  = Cat(Fill(20, io.in.bits.inst(31)), io.in.bits.inst(31, 25), io.in.bits.inst(11, 7))
    val imm_b  = Cat(Fill(20, io.in.bits.inst(31)), io.in.bits.inst(31), io.in.bits.inst(7), io.in.bits.inst(30, 25), io.in.bits.inst(11, 8), 0.B)
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
        ASel.  PC.id.U -> io.in.bits.pc,
        ASel.GPR1.id.U -> io.gpr_rdata1,
        ASel. CSR.id.U -> io.csr_rdata,
    ))
    val rs2 = MuxLookup(op.bSel, 0.U(32.W))(Seq(
        BSel.GPR1.id.U -> io.gpr_rdata1,
        BSel.GPR2.id.U -> io.gpr_rdata2,
        BSel. Imm.id.U -> imm,
        BSel. CSR.id.U -> io.csr_rdata
    ))
    io.out.bits.rs1 := rs1
    io.out.bits.rs2 := rs2
    io.out.bits.rs3 := Mux(op.cSel, io.in.bits.snpc, io.gpr_rdata1)
    io.out.bits.rs4 := io.gpr_rdata2

    io.out.bits.exu_tag := op.exuTag
    io.out.bits.alu_bsel := op.aluBSel
    io.out.bits.alu_add := op.aluAdd
    io.out.bits.is_branch := op.isBranch
    io.out.bits.is_jmp := op.isJmp

    // LSU
    io.out.bits.mem_wen := op.memWen
    io.out.bits.mem_ren := op.memRen

    // WBU
    // GPR
    io.out.bits.gpr_waddr := io.in.bits.inst(7 + Config.GPRAddrLength - 1, 7)
    io.out.bits.gpr_ws := op.gprWSel
    io.out.bits.gpr_wen := op.gprWen
    
    // CSR
    io.out.bits.cause_en := false.B
    // io.out.bits.csr_ws := op.csrWSel
    io.out.bits.csr_wen := op.csrWen
    io.out.bits.csr_waddr := MuxLookup(op.csrWAddrSel, 0.U(Config.CSRAddrLength.W))(Seq(
        CSRAddrSel.VEC.id.U -> CSRAddr.MTVEC,
        CSRAddrSel.EPC.id.U -> CSRAddr.MEPC,
        CSRAddrSel.Ins.id.U -> csr_addr_translate(io.in.bits.inst(31, 20))
    ))
    io.out.bits.dnpc_sel := op.dnpcSel

    // FENCE
    io.fence_i := op.fenceI && io.in.valid

    // TAG
    io.out.bits.is_ivd := op.isIvd && !reset.asBool
    io.out.bits.is_brk := op.isBrk

    io. in.ready := io.out.ready && !io.raw
    io.out.valid := io.in.valid && !io.predict_failed && !io.raw

    // DEBUG
    io.out.bits.dbg <> io.in.bits.dbg
}
