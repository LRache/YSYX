package cpu.idu

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import cpu.Config
import cpu.reg.CSRAddr
import cpu.IFUMessage
import cpu.IDUMessage
import cpu.reg.CSR
import chisel3.internal.castToInt
import cpu.idu.CGPRRaddr1Sel.INST1

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

class InstDecoderOut extends Bundle {
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
    
    val fence_i = Bool()
}

object InstDecode {
    def apply(inst: UInt, out: InstDecoderOut) : Unit = {
        if (inst.getWidth != 32) {
            throw new IllegalArgumentException
        }

        val op = Decoder.decode(inst)
        
        // IDU
        out.gpr_raddr1 := inst(15 + Config.GPRAddrLength - 1, 15)
        out.gpr_raddr2 := inst(20 + Config.GPRAddrLength - 1, 20)
        out.gpr_ren1 := op.gprRen1
        out.gpr_ren2 := op.gprRen2
        out.csr_raddr := MuxLookup(op.csrRAddrSel, 0.U(12.W))(Seq(
            CSRAddrSel.VEC.U -> CSRAddr.MTVEC,
            CSRAddrSel.EPC.U -> CSRAddr.MEPC,
            CSRAddrSel.Ins.U -> CSRAddr.csr_addr_translate(inst(31, 20))
        ))
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
                ImmType. I.U -> imm_i,
                ImmType.IU.U -> imm_iu,
                ImmType. S.U -> imm_s,
                ImmType. U.U -> imm_u,
                ImmType. B.U -> imm_b,
                ImmType. J.U -> imm_j,
                ImmType. C.U -> imm_c,
            )
        )
        out.imm := imm

        out.aSel := op.aSel
        out.bSel := op.bSel
        out.cSel := op.cSel
        out.dSel := op.dSel

        // EXU
        out.func3       := inst(14, 12)
        out.exu_tag     := op.exuTag
        out.alu_add     := op.aluAdd
        out.is_branch   := op.isBranch
        out.is_jmp      := op.isJmp
        out.dnpc_sel    := op.dnpcSel

        // LSU
        out.mem_ren := op.memRen
        out.mem_wen := op.memWen

        // WBU
        out.gpr_waddr   := Mux(op.gprWen, inst(7 + Config.GPRAddrLength - 1, 7), 0.U)
        out.gpr_ws      := op.gprWSel
        out.csr_wen     := op.csrWen && out.gpr_raddr1.orR
        out.csr_waddr   := CSRAddr.csr_addr_translate(inst(31, 20))
        out.is_trap     := op.isTrap

        out.fence_i := op.fenceI
        out.is_brk  := op.isBrk
        out.is_ivd  := op.isIvd
    }
}

object CInstDecode {
    def decode(op: CExtensionOP, out: InstDecoderOut) : Unit = {

    }
    
    def apply(inst: UInt, out: InstDecoderOut) : Unit = {
        def gpr_addr_translate(caddr: UInt) : UInt = {
            if (caddr.getWidth != 3) throw new IllegalArgumentException
            return Cat("b01".U, caddr)
        }
        
        if (inst.getWidth != 16) {
            throw new IllegalArgumentException
        }

        val op = CExtensionDecoder.decode(inst)

        // IDU
        val gpr_addr_1 = inst(11, 7)
        val gpr_addr_2 = gpr_addr_translate(inst(9, 7))
        val gpr_addr_3 = inst(6, 2)
        val gpr_addr_4 = gpr_addr_translate(inst(4, 2))

        out.gpr_raddr1 := MuxLookup(op.gprRaddr1, 0.U)(Seq(
            CGPRRaddr1Sel.INST1.U -> gpr_addr_1,
            CGPRRaddr1Sel.INST2.U -> gpr_addr_2,
            CGPRRaddr1Sel.   X2.U -> 2.U(5.W),
        ))
        out.gpr_raddr2 := MuxLookup(op.gprRaddr2, 0.U)(Seq(
            CGPRRaddr2Sel.INST3.U -> gpr_addr_3,
            CGPRRaddr2Sel.INST4.U -> gpr_addr_4,
            CGPRRaddr2Sel.   X0.U -> 0.U(5.W),
        ))
        out.gpr_ren1 := op.gprRen1
        out.gpr_ren2 := op.gprRen2
        out.csr_raddr := DontCare
        out.csr_ren := false.B

        val imm_sl  = Cat(0.U(24.W), inst(3, 2), inst(12), inst(6, 4), 0.U(2.W))
        val imm_ss  = Cat(0.U(24.W), inst(8, 7), inst(12, 9), 0.U(2.W))
        val imm_rls = Cat(0.U(25.W), inst(5), inst(12, 10), inst(6), 0.U(2.W))
        val imm_ji  = Cat(Fill(20, inst(12)), inst(12), inst(8), inst(10, 9), inst(6), inst(7), inst(2), inst(11), inst(5, 3), 0.U(1.W))
        val imm_b   = Cat(Fill(23, inst(12)), inst(12), inst(6, 5), inst(11, 10), inst(4, 3), 0.U(1.W))
        val imm_li  = Cat(Fill(26, inst(12)), inst(12), inst(6, 2))
        val imm_ui  = Cat(Fill(14, inst(12)), inst(6, 2), 0.U(12.W))
        val imm_au  = Cat(0.U(26.W), inst(12), inst(6, 2))
        val imm_as  = imm_li
        val imm_i16 = Cat(0.U(22.W), inst(15), inst(4, 3), inst(5), inst(2), inst(6), 0.U(4.W))
        val imm_i4  = Cat(0.U(22.W), inst(11, 8), inst(13, 12), inst(5), inst(6), 0.U(2.W))
        out.imm := MuxLookup(op.immType, 0.U)(Seq(
            CImmType. SL.id.U -> imm_sl,
            CImmType. SS.id.U -> imm_ss,
            CImmType.RLS.id.U -> imm_rls,
            CImmType. JI.id.U -> imm_ji,
            CImmType.  B.id.U -> imm_b,
            CImmType. LI.id.U -> imm_li,
            CImmType. UI.id.U -> imm_ui,
            CImmType. AU.id.U -> imm_au,
            CImmType. AS.id.U -> imm_as,
            CImmType.I16.id.U -> imm_i16,
            CImmType. I4.id.U -> imm_i4
        ))

        out.aSel := op.aSel
        out.bSel := op.bSel
        out.cSel := op.cSel
        out.dSel := false.B

        // EXU
        out.func3   := op.func3
        out.exu_tag := op.exuTag
        out.alu_add := op.aluAdd
        out.is_branch := op.isBranch
        out.is_jmp  := op.isJmp
        out.dnpc_sel := false.B
        
        // LSU
        out.mem_ren := op.memRen
        out.mem_wen := op.memWen

        // WBU
        out.gpr_waddr := MuxLookup(op.gprWaddr, 0.U)(Seq(
            CGPRWaddrSel.INST1.U -> gpr_addr_1,
            CGPRWaddrSel.INST2.U -> gpr_addr_2,
            CGPRWaddrSel.INST4.U -> gpr_addr_4,
            CGPRWaddrSel.   X1.U -> 1.U(5.W),
            CGPRWaddrSel.   X2.U -> 2.U(5.W),
        ))
        out.gpr_ws := op.gprWSel
        out.csr_waddr := DontCare
        out.csr_wen := false.B

        out.is_trap := false.B
        out.is_ivd  := op.isIvd
        out.is_brk  := op.isBrk
        out.fence_i := false.B
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

        val isCompressed = false.B
        val halfInst = inst(15, 0)

        val normalInstOp = Wire(new InstDecoderOut())
        // val compressedInstOp = Wire(new InstDecoderOut())
        InstDecode (inst,     normalInstOp)
        // CInstDecode(halfInst, compressedInstOp)

        // val op = Mux(isCompressed, compressedInstOp, normalInstOp)
        val op = normalInstOp
        
        // EXU
        out.bits.func3 := op.func3

        gpr_raddr1 := op.gpr_raddr1
        gpr_raddr2 := op.gpr_raddr2
        gpr_ren1 := op.gpr_ren1
        gpr_ren2 := op.gpr_ren2
        csr_raddr := op.csr_raddr
        csr_ren := op.csr_ren

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
