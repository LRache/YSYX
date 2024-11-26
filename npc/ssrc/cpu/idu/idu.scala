package cpu.idu

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import cpu.Config
import cpu.reg.CSRAddr
import cpu.IFUMessage
import cpu.IDUMessage
import cpu.reg.CSR

class IDUWire extends Bundle {
    val in =  Decoupled(new IFUMessage)
    val out = Decoupled(new IDUMessage)

    val gpr_raddr1 = UInt(Config.GPRAddrLength.W)
    val gpr_raddr2 = UInt(Config.GPRAddrLength.W)
    val gpr_rdata1 = UInt(32.W)
    val gpr_rdata2 = UInt(32.W)
    val gpr_ren1   = Bool()
    val gpr_ren2   = Bool()
    val csr_raddr  = UInt(Config.CSRAddrLength.W)
    val csr_rdata  = UInt(32.W)
    val csr_ren    = Bool()
        
    val fence_i = Bool()

    val raw = Bool()
    val predict_failed = Bool()
}

class InstDecoderOut extends Bundle{
    val gpr_raddr1 = UInt(Config.GPRAddrLength.W)
    val gpr_raddr2 = UInt(Config.GPRAddrLength.W)
    val gpr_ren1   = Bool()
    val gpr_ren2   = Bool()
    val csr_raddr  = UInt(Config.CSRAddrLength.W)
    val csr_ren    = Bool()

    val imm = UInt(32.W)

    val aSel = UInt(2.W)
    val bSel = UInt(2.W)
    val cSel = Bool()
    val dSel = Bool()

    val func3   = UInt(3.W)
    val exu_tag = Bool()
    val alu_add = Bool()
    val is_branch = Bool()
    val is_jmp   = Bool()
    val dnpc_sel = Bool()

    val mem_wen = Bool()
    val mem_ren = Bool()

    val gpr_waddr = UInt(Config.GPRAddrLength.W)
    val gpr_ws    = UInt(2.W)
    val csr_waddr = UInt(Config.CSRAddrLength.W)
    val csr_wen   = Bool()
    
    val is_trap = Bool()
    val is_ivd  = Bool()
    val is_brk  = Bool()
    
    val fence_i    = Bool()
}

object InstDecode {
    def apply(inst: UInt, out: InstDecoderOut) : Unit = {
        val op = Decoder.decode(inst)
        
        out.func3 := inst(14, 12)
        out.gpr_raddr1 := inst(15 + Config.GPRAddrLength - 1, 15)
        out.gpr_raddr2 := inst(20 + Config.GPRAddrLength - 1, 20)
        out.gpr_ren1 := op.gprRen1
        out.gpr_ren2 := op.gprRen2
        out.csr_raddr := MuxLookup(op.csrRAddrSel, 0.U(12.W))(Seq(
            CSRAddrSel.VEC.id.U -> CSRAddr.MTVEC,
            CSRAddrSel.EPC.id.U -> CSRAddr.MEPC,
            CSRAddrSel.Ins.id.U -> CSRAddr.csr_addr_translate(inst(31, 20))
            )
        )
        out.csr_ren := op.csrRen

        val signExtend20 = Fill(20, inst(31))
        val imm_i  = Cat(signExtend20, inst(31, 20))
        val imm_iu = Cat(0.U(20.W), inst(31, 20))
        val imm_s  = Cat(signExtend20, inst(31, 25), inst(11, 7))
        val imm_b  = Cat(signExtend20, inst(31), inst(7), inst(30, 25), inst(11, 8), 0.B)
        val imm_u  = Cat(inst(31, 12), 0.U(12.W))
        val imm_j  = Cat(Fill(11, inst(31)), inst(31), inst(19, 12), inst(20), inst(30, 21), 0.B)
        val imm_c  = Cat(0.U(27.W), inst(19, 15))
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
        out.imm := imm

        out.aSel := op.aSel
        out.bSel := op.bSel
        out.cSel := op.cSel
        out.dSel := op.dSel

        out.exu_tag := op.exuTag
        out.alu_add := op.aluAdd
        out.is_branch := op.isBranch
        out.is_jmp := op.isJmp
        out.dnpc_sel := op.dnpcSel

        out.mem_ren := op.memRen
        out.mem_wen := op.memWen

        out.gpr_waddr := Mux(op.gprWen, inst(7 + Config.GPRAddrLength - 1, 7), 0.U)
        out.gpr_ws := op.gprWSel
        out.csr_wen := op.csrWen && out.gpr_raddr1.orR
        out.csr_waddr := CSRAddr.csr_addr_translate(inst(31, 20))
        out.is_trap := op.isTrap

        out.fence_i := op.fenceI
        out.is_brk := op.isBrk
        out.is_ivd := op.isIvd
    }
}

object IDUInline {
    def apply(
        in:  DecoupledIO[IFUMessage],
        out: DecoupledIO[IDUMessage],
        gpr_raddr1: UInt,
        gpr_raddr2: UInt,
        gpr_rdata1: UInt,
        gpr_rdata2: UInt,
        gpr_ren1  : Bool,
        gpr_ren2  : Bool,
        csr_raddr : UInt,
        csr_rdata : UInt,
        csr_ren   : Bool,
        fence_i   : Bool,
        raw       : Bool,
        predict_failed: Bool
    ): Unit = {
        val pc   = in.bits.pc
        val snpc = Wire(UInt(32.W))
        val inst = in.bits.inst

        if (Config.HasBTB) {
            snpc := pc + 4.U(32.W)
        } else {
            snpc := in.bits.snpc
        }

        val normalInstOp = Wire(new InstDecoderOut())
        InstDecode(inst, normalInstOp)

        val op = normalInstOp
        
        // EXU
        out.bits.func3 := op.func3

        gpr_raddr1 := op.gpr_raddr1
        gpr_raddr2 := op.gpr_raddr2
        gpr_ren1 := op.gpr_ren1
        gpr_ren2 := op.gpr_ren2
        csr_raddr := op.csr_raddr
        csr_ren := op.csr_ren

        // val signExtend20 = Fill(20, in.bits.inst(31))
        // val imm_i  = Cat(signExtend20, in.bits.inst(31, 20))
        // val imm_iu = Cat(0.U(20.W), in.bits.inst(31, 20))
        // val imm_s  = Cat(signExtend20, in.bits.inst(31, 25), in.bits.inst(11, 7))
        // val imm_b  = Cat(signExtend20, in.bits.inst(31), in.bits.inst(7), in.bits.inst(30, 25), in.bits.inst(11, 8), 0.B)
        // val imm_u  = Cat(in.bits.inst(31, 12), 0.U(12.W))
        // val imm_j  = Cat(Fill(11, in.bits.inst(31)), in.bits.inst(31), in.bits.inst(19, 12), in.bits.inst(20), in.bits.inst(30, 21), 0.B)
        // val imm_c  = Cat(0.U(27.W), in.bits.inst(19, 15))
        // val imm = MuxLookup(op.immType, 0.U(32.W))(Seq(
        //         ImmType. I.id.U -> imm_i,
        //         ImmType.IU.id.U -> imm_iu,
        //         ImmType. S.id.U -> imm_s,
        //         ImmType. U.id.U -> imm_u,
        //         ImmType. B.id.U -> imm_b,
        //         ImmType. J.id.U -> imm_j,
        //         ImmType. C.id.U -> imm_c,
        //     )
        // )

        
        val rs1 = MuxLookup(op.aSel, 0.U(32.W))(Seq(
            ASel.  PC.U -> pc,
            ASel.GPR1.U -> gpr_rdata1,
            ASel. CSR.U -> csr_rdata,
            ASel.ZERO.U -> 0.U
        ))
        val rs2 = MuxLookup(op.bSel, 0.U(32.W))(Seq(
            BSel.GPR1.U -> gpr_rdata1,
            BSel.GPR2.U -> gpr_rdata2,
            BSel. Imm.U -> op.imm,
            BSel. CSR.U -> csr_rdata
        ))
        out.bits.rs1 := rs1
        out.bits.rs2 := rs2
        out.bits.rs3 := Mux(op.cSel, snpc, gpr_rdata1)
        out.bits.rs4 := Mux(op.dSel, op.imm,  gpr_rdata2)

        out.bits.exu_tag := op.exu_tag
        out.bits.alu_add := op.alu_add
        out.bits.is_branch := op.is_branch
        out.bits.is_jmp := op.is_jmp
        out.bits.dnpc_sel := op.dnpc_sel
        
        out.bits.predictor_pc := in.bits.pc
        out.bits.predict_jmp := in.bits.predict_jmp

        // LSU
        out.bits.mem_wen := op.mem_wen
        out.bits.mem_ren := op.mem_ren

        // WBU
        // GPR
        out.bits.gpr_waddr := op.gpr_waddr
        out.bits.gpr_ws := op.gpr_ws
        
        // CSR
        out.bits.trap.is_trap := op.is_ivd || op.is_trap
        out.bits.trap.is_interrupt := false.B
        out.bits.trap.cause := Mux(op.is_ivd, 0x2.U(4.W), 0xb.U(4.W))
        
        out.bits.csr_wen := op.csr_wen
        out.bits.csr_waddr := op.csr_waddr

        // FENCE
        fence_i := op.fence_i && in.valid

        // TAG
        out.bits.is_ivd := op.is_ivd
        out.bits.is_brk := op.is_brk

         in.ready := out.ready && !raw
        out.valid := in .valid && !predict_failed && !raw

        // DEBUG
        out.bits.dbg <> in.bits.dbg
    }

    def apply(wire: IDUWire) : Unit = {
        apply(
            in  = wire.in ,
            out = wire.out,
            gpr_raddr1 = wire.gpr_raddr1,
            gpr_raddr2 = wire.gpr_raddr2,
            gpr_rdata1 = wire.gpr_rdata1,
            gpr_rdata2 = wire.gpr_rdata2,
            gpr_ren1   = wire.gpr_ren1,
            gpr_ren2   = wire.gpr_ren2,
            csr_raddr  = wire.csr_raddr,
            csr_rdata  = wire.csr_rdata,
            csr_ren    = wire.csr_ren,
            fence_i    = wire.fence_i,
            raw        = wire.raw,
            predict_failed = wire.predict_failed
        )
    }
}

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
    
    IDUInline(
        in  = io.in,
        out = io.out,
        gpr_raddr1 = io.gpr_raddr1,
        gpr_raddr2 = io.gpr_raddr2,
        gpr_rdata1 = io.gpr_rdata1,
        gpr_rdata2 = io.gpr_rdata2,
        gpr_ren1   = io.gpr_ren1,
        gpr_ren2   = io.gpr_ren2,
        csr_raddr  = io.csr_raddr,
        csr_rdata  = io.csr_rdata,
        csr_ren    = io.csr_ren,
        fence_i    = io.fence_i,
        raw        = io.raw,
        predict_failed = io.predict_failed
    )
}
