package cpu.idu

import chisel3._
import chisel3.util._

import cpu.reg.CSRAddr
import cpu.IFUMessage
import cpu.IDUMessage
import Encode.Pos

class IDU extends Module {
   val io = IO(new Bundle {
      val in   = Flipped(Decoupled(new IFUMessage))
      val out  = Decoupled(new IDUMessage)
      
      val reg_raddr1 = Output(UInt(5.W))
      val reg_raddr2 = Output(UInt(5.W))
      val reg_rdata1 = Input (UInt(32.W))
      val reg_rdata2 = Input (UInt(32.W))

      val csr_raddr  = Output(UInt(12.W))
      val csr_rdata  = Input (UInt(32.W))

      val dbg  = Output(UInt(32.W))
   })
   io.out.bits.rs1 := io.reg_rdata1
   io.out.bits.rs2 := io.reg_rdata2
   io.out.bits.rd := io.in.bits.inst(11, 7)

   val imm_i   = Cat(Fill(20, io.in.bits.inst(31)), io.in.bits.inst(31, 20))
   val imm_iu  = Cat(0.U(20.W), io.in.bits.inst(31, 20))
   val imm_s   = Cat(Fill(20, io.in.bits.inst(31)), io.in.bits.inst(31, 25), io.in.bits.inst(11, 7))
   val imm_b   = Cat(Fill(20, io.in.bits.inst(31)), io.in.bits.inst(31), io.in.bits.inst(7), io.in.bits.inst(30, 25), io.in.bits.inst(11, 8), 0.B)
   val imm_u   = Cat(io.in.bits.inst(31, 12), 0.U(12.W))
   val imm_j   = Cat(Fill(11, io.in.bits.inst(31)), io.in.bits.inst(31), io.in.bits.inst(19, 12), io.in.bits.inst(20), io.in.bits.inst(30, 21), 0.B)

   val op = Decoder.decode(io.in.bits.inst)
   val is_ecall = op(Pos.IsECall).asBool
   
   val imm_type = op(Pos.ImmType + Pos.ImmTypeL - 1, Pos.ImmType)
   io.out.bits.alu_sel  := op(Pos.ALUSel  + Pos.ALUSelL  - 1, Pos.ALUSel )
   io.out.bits.a_sel    := op(Pos.ASel).asBool
   io.out.bits.b_sel    := op(Pos.BSel).asBool
   io.out.bits.cmp_sel  := op(Pos.CmpSel  + Pos.CmpSelL  - 1, Pos.CmpSel)
   io.out.bits.is_jmp   := op(Pos.IsJmp).asBool
   io.out.bits.imm := MuxLookup(imm_type, 0.U(32.W))(Seq (
        ImmType. I.id.U -> imm_i,
        ImmType.IU.id.U -> imm_iu,
        ImmType. S.id.U -> imm_s,
        ImmType. U.id.U -> imm_u,
        ImmType. B.id.U -> imm_b,
        ImmType. J.id.U -> imm_j
   ))

   val csr_raddr_sel = op(Pos.CSRRAddrSel + Pos.CSRRAddrSelL - 1, Pos.CSRRAddrSel)
   io.csr_raddr := MuxLookup(csr_raddr_sel, 0.U(12.W))(Seq (
        CSRAddrSel.  N.id.U -> 0.U(12.W),
        CSRAddrSel.VEC.id.U -> CSRAddr.MTVEC,
        CSRAddrSel.EPC.id.U -> CSRAddr.MEPC,
        CSRAddrSel.Ins.id.U -> io.in.bits.inst(31, 20)
   ))
   io.out.bits.csr_rdata := io.csr_rdata

   val csr_waddr_sel = op(Pos.CSRWAddrSel + Pos.CSRWAddrSelL - 1, Pos.CSRWAddrSel)
   io.out.bits.csr_wen1 := ((op(Pos.CSRWen) && !(io.reg_raddr1 === 0.U)) || is_ecall)
   io.out.bits.csr_wen2 := is_ecall
   io.out.bits.csr_imm  := Cat(0.U(27.W), io.in.bits.inst(19, 15))
   io.out.bits.csr_ws   := op(Pos.CSRWSel + Pos.CSRWSelL - 1, Pos.CSRWSel)
   io.out.bits.csr_waddr1 := MuxLookup(csr_waddr_sel, 0.U(12.W))(Seq (
        CSRAddrSel.  N.id.U -> 0.U(12.W),
        CSRAddrSel.VEC.id.U -> CSRAddr.MTVEC,
        CSRAddrSel.EPC.id.U -> CSRAddr.MEPC,
        CSRAddrSel.Ins.id.U -> io.in.bits.inst(31, 20)
   ))
   io.out.bits.csr_waddr2 := CSRAddr.MCAUSE
   io.out.bits.csr_wd_sel := is_ecall

   io.out.bits.mem_type := op(Pos.MemType + Pos.MemTypeL - 1, Pos.MemType)
   io.out.bits.mem_wen  := op(Pos.MemWen).asBool && io.in.valid
   io.out.bits.mem_ren  := op(Pos.MemRen).asBool
   
   io.out.bits.reg_ws   := op(Pos.RegWSel + Pos.RegWSelL - 1, Pos.RegWSel)
   io.out.bits.reg_wen  := op(Pos.RegWen).asBool && io.in.valid
   
   io.out.bits.is_brk   := op(Pos.IsBrk).asBool

   io.out.bits.dnpc_sel := op(Pos.DNPCSel).asBool

   io.reg_raddr1 := io.in.bits.inst(19, 15)
   io.reg_raddr2 := Mux(is_ecall, 15.U(5.W), io.in.bits.inst(24, 20))
   
   io.out.bits.is_ivd := Mux(reset.asBool, 0.U, op(Pos.IsIvd).asBool)

   io.dbg := imm_j

   // Passthrough
   io.out.bits.pc    := io.in.bits.pc
   io.out.bits.snpc  := io.in.bits.snpc

   io. in.ready := io.out.ready
   io.out.valid := io. in.valid
}
