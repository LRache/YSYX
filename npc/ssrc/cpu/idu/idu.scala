package cpu.idu

import chisel3._
import chisel3.util._

import cpu.Config
import cpu.reg.CSRAddr
import cpu.IFUMessage
import cpu.IDUMessage
import cpu.reg.CSR
import Encode.Pos

class IDU extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new IFUMessage))
    val out = Decoupled(new IDUMessage)

    val gpr_raddr1 = Output(UInt(Config.GPRAddrLength.W))
    val gpr_raddr2 = Output(UInt(Config.GPRAddrLength.W))
    val gpr_rdata1 = Input (UInt(32.W))
    val gpr_rdata2 = Input (UInt(32.W))
    // val csr_raddr = Output(UInt(4.W))
    val fence_i = Output(Bool())

    val raw = Input(Bool())
    val predict_failed = Input(Bool())
  })

    val op = Decoder.decode(io.in.bits.inst)
    val is_ecall = op.isEcall

    // io.out.bits.gpr_raddr1 := io.in.bits.inst(15 + Config.GPRAddrLength - 1, 15)
    // io.out.bits.gpr_raddr2 := Mux(is_ecall, 15.U(5.W), io.in.bits.inst(20 + Config.GPRAddrLength - 1, 20))
    io.gpr_raddr1 := io.in.bits.inst(15 + Config.GPRAddrLength - 1, 15)
    io.gpr_raddr2 := Mux(is_ecall, 15.U(5.W), io.in.bits.inst(20 + Config.GPRAddrLength - 1, 20))
    io.out.bits.gpr_rdata1 := io.gpr_rdata1
    io.out.bits.gpr_rdata2 := io.gpr_rdata2
    when(io.in.valid) {
        printf("IDU 0x%x %d\n", io.out.bits.dbg.pc, io.out.bits.gpr_waddr)
    }

    def csr_addr_translate(origin: UInt): UInt = {
      MuxLookup(origin, CSRAddr.NONE) (Seq(
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
    io.out.bits.csr_raddr := MuxLookup(op.csrRASel, 0.U(12.W))(
        Seq(
          CSRAddrSel.N.id.U   -> CSRAddr.NONE,
          CSRAddrSel.VEC.id.U -> CSRAddr.MTVEC,
          CSRAddrSel.EPC.id.U -> CSRAddr.MEPC,
          CSRAddrSel.Ins.id.U -> csr_addr_translate(io.in.bits.inst(31, 20))
        )
    )

    // EXU
    io.out.bits.alu_sel := op.aluSel
    io.out.bits.a_sel := op.aSel
    io.out.bits.b_sel := op.bSel
    io.out.bits.cmp_sel := op.cmpSel

    val imm_i   = Cat(Fill(20, io.in.bits.inst(31)), io.in.bits.inst(31, 20))
    val imm_iu  = Cat(0.U(20.W), io.in.bits.inst(31, 20))
    val imm_s   = Cat(Fill(20, io.in.bits.inst(31)), io.in.bits.inst(31, 25), io.in.bits.inst(11, 7))
    val imm_b   = Cat(Fill(20, io.in.bits.inst(31)), io.in.bits.inst(31), io.in.bits.inst(7), io.in.bits.inst(30, 25), io.in.bits.inst(11, 8), 0.B)
    val imm_u   = Cat(io.in.bits.inst(31, 12), 0.U(12.W))
    val imm_j   = Cat(Fill(11, io.in.bits.inst(31)), io.in.bits.inst(31), io.in.bits.inst(19, 12), io.in.bits.inst(20), io.in.bits.inst(30, 21), 0.B)
    val imm_c   = Cat(0.U(27.W), io.in.bits.inst(19, 15))
    io.out.bits.imm := MuxLookup(op.immType, 0.U(32.W))(Seq(
            ImmType.I.id.U -> imm_i,
            ImmType.IU.id.U -> imm_iu,
            ImmType.S.id.U -> imm_s,
            ImmType.U.id.U -> imm_u,
            ImmType.B.id.U -> imm_b,
            ImmType.J.id.U -> imm_j,
            ImmType.C.id.U -> imm_c,
        )
    )

    // LSU
    io.out.bits.mem_type := io.in.bits.inst(14, 12)
    io.out.bits.mem_wen := op.memWen
    io.out.bits.mem_ren := op.memRen

    // WBU
    // GPR
    io.out.bits.gpr_waddr := io.in.bits.inst(7 + Config.GPRAddrLength - 1, 7)
    io.out.bits.gpr_ws := op.gprWSel
    io.out.bits.gpr_wen := op.gprWen
    
    // CSR
    io.out.bits.is_ecall := is_ecall
    io.out.bits.csr_ws := op.csrWSel
    io.out.bits.csr_waddr := MuxLookup(op.csrWASel, 0.U(12.W))(Seq(
        CSRAddrSel.  N.id.U -> CSRAddr.NONE,
        CSRAddrSel.VEC.id.U -> CSRAddr.MTVEC,
        CSRAddrSel.EPC.id.U -> CSRAddr.MEPC,
        CSRAddrSel.Ins.id.U -> csr_addr_translate(io.in.bits.inst(31, 20))
    ))
    io.out.bits.csr_wd_sel := op.isEcall
    // val csr_ws =op.csrWSel
    // val csr_waddr = MuxLookup(op.csrWASel, 0.U(12.W))(Seq(
    //     CSRAddrSel.  N.id.U -> CSRAddr.NONE,
    //     CSRAddrSel.VEC.id.U -> CSRAddr.MTVEC,
    //     CSRAddrSel.EPC.id.U -> CSRAddr.MEPC,
    //     CSRAddrSel.Ins.id.U -> csr_addr_translate(io.in.bits.inst(31, 20))
    // ))
    // val csr_wd_sel = op.isEcall

    io.out.bits.dnpc_sel := op.dnpcSel

    // FENCE
    io.fence_i := op.isFenceI && io.in.valid

    // TAG
    io.out.bits.is_ivd := Mux(reset.asBool, 0.U, op.isIvd)
    io.out.bits.is_brk := op.isBrk
    // val is_ivd = op.isIvd
    // val is_brk = op.isBrk

    // Passthrough
    io.out.bits.pc := io.in.bits.pc
    io.out.bits.snpc := io.in.bits.snpc

    io. in.ready := io.out.ready && !io.raw
    io.out.valid := io.in.valid && !io.predict_failed && !io.raw

    // when(io.in.valid) {
    //     printf("in valid: IDU %d %x\n", io.out.bits.gpr_waddr, io.in.bits.pc)
    // }
    // when(io.in.valid && io.out.ready) {
    //     printf("set IDU %d\n", io.out.bits.rd)
    // }
    // DEBUG
    io.out.bits.dbg.pc := io.in.bits.dbg.pc
    // when(io.in.valid) {
    //     printf("IDU 0x%x\n", io.in.bits.dbg.pc)
    // }
}
