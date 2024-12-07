package cpu.idu

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import cpu.Config
import cpu.reg.CSRAddr
import cpu.IFUMessage
import cpu.IDUMessage
import cpu.reg.CSR

class IDUBundle extends Bundle {
    val in =  Decoupled(new IFUMessage)
    val out = Decoupled(new IDUMessage)

    val gpr_raddr1 = UInt(Config.GPRAddrWidth.W)
    val gpr_raddr2 = UInt(Config.GPRAddrWidth.W)
    val gpr_rdata1 = UInt(32.W)
    val gpr_rdata2 = UInt(32.W)
    val gpr_ren1   = Bool()
    val gpr_ren2   = Bool()
    val csr_raddr  = UInt(Config.CSRAddrWidth.W)
    val csr_rdata  = UInt(32.W)
    val csr_ren    = Bool()
        
    val fence_i = Bool()

    val raw = Bool()
    val predict_failed = Bool()
}

class InstDecoderOut extends Bundle {
    val gpr_raddr1 = UInt(Config.GPRAddrWidth.W)
    val gpr_raddr2 = UInt(Config.GPRAddrWidth.W)
    val gpr_ren1   = Bool()
    val gpr_ren2   = Bool()
    val csr_raddr  = UInt(Config.CSRAddrWidth.W)
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

    val gpr_waddr = UInt(Config.GPRAddrWidth.W)
    val gpr_ws    = UInt(2.W)
    val gpr_wen   = Bool()
    val csr_waddr = UInt(Config.CSRAddrWidth.W)
    val csr_wen   = Bool()
    
    val is_trap = Bool()
    val is_ivd  = Bool()
    val is_brk  = Bool()
    
    val fence_i = Bool()
}

object InstDecode {
    def apply(inst: UInt, out: InstDecoderOut) : Unit = {
        assert(inst.getWidth == 32)

        val op = Wire(new DecodeOPBundle)
        InstDecoder.decode(inst, op)
        
        // IDU
        out.gpr_raddr1 := inst(15 + Config.GPRAddrWidth - 1, 15)
        out.gpr_raddr2 := inst(20 + Config.GPRAddrWidth - 1, 20)
        out.gpr_ren1 := op.gprRen1
        out.gpr_ren2 := op.gprRen2
        out.csr_raddr := MuxLookup(op.csrRAddrSel, 0.U(Config.CSRAddrWidth.W))(Seq(
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
        out.gpr_waddr   := inst(7 + Config.GPRAddrWidth - 1, 7)
        out.gpr_ws      := op.gprWSel
        out.gpr_wen     := op.gprWen
        out.csr_wen     := op.csrWen && out.gpr_raddr1.orR
        out.csr_waddr   := CSRAddr.csr_addr_translate(inst(31, 20))
        out.is_trap     := op.isTrap

        out.fence_i := op.fenceI
        out.is_brk  := op.isBrk
        out.is_ivd  := op.isIvd
    }
}

object CInstDecode {
    def apply(inst: UInt, out: InstDecoderOut) : Unit = {
        def gpr_addr_translate(caddr: UInt) : UInt = {
            if (caddr.getWidth != 3) throw new IllegalArgumentException
            return Cat((0.U)((5 - Config.GPRAddrWidth).W), 1.U(1.W), caddr)
        }
        
        if (inst.getWidth != 16) {
            throw new IllegalArgumentException
        }

        val op = Wire(new CDecodeOPBundle)
        CInstDecoder.decode(inst, op)

        // IDU
        val gpr_addr_1 = inst(11, 7)
        val gpr_addr_2 = gpr_addr_translate(inst(9, 7))
        val gpr_addr_3 = inst(6, 2)
        val gpr_addr_4 = gpr_addr_translate(inst(4, 2))

        out.gpr_raddr1 := MuxLookup(op.gprRaddr1, 0.U)(Seq(
            CGPRRaddr1Sel.INST1.U -> gpr_addr_1,
            CGPRRaddr1Sel.INST2.U -> gpr_addr_2,
            CGPRRaddr1Sel.   X2.U -> 2.U(Config.GPRAddrWidth.W),
            CGPRRaddr1Sel. ZERO.U -> 0.U(Config.GPRAddrWidth.W),
        ))
        out.gpr_raddr2 := MuxLookup(op.gprRaddr2, 0.U)(Seq(
            CGPRRaddr2Sel.INST3.U -> gpr_addr_3,
            CGPRRaddr2Sel.INST4.U -> gpr_addr_4,
            CGPRRaddr2Sel. ZERO.U -> 0.U(Config.GPRAddrWidth.W),
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
        val imm_au  = Cat(Fill(27, inst(12)), inst(6, 2))
        val imm_as  = imm_li
        val imm_i16 = Cat(Fill(23, inst(12)), inst(4, 3), inst(5), inst(2), inst(6), 0.U(4.W))
        val imm_i4  = Cat(0.U(22.W), inst(11, 8), inst(13, 12), inst(5), inst(6), 0.U(2.W))
        out.imm := MuxLookup(op.immType, 0.U)(Seq(
            CImmType. SL.U -> imm_sl,
            CImmType. SS.U -> imm_ss,
            CImmType.RLS.U -> imm_rls,
            CImmType. JI.U -> imm_ji,
            CImmType.  B.U -> imm_b,
            CImmType. LI.U -> imm_li,
            CImmType. UI.U -> imm_ui,
            CImmType. AU.U -> imm_au,
            CImmType. AS.U -> imm_as,
            CImmType.I16.U -> imm_i16,
            CImmType. I4.U -> imm_i4
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
        out.gpr_waddr := MuxLookup(op.gprWaddrSel, 0.U)(Seq(
            CGPRWaddrSel.INST1.U -> gpr_addr_1,
            CGPRWaddrSel.INST2.U -> gpr_addr_2,
            CGPRWaddrSel.INST4.U -> gpr_addr_4,
            CGPRWaddrSel.   X1.U -> 1.U(5.W),
            CGPRWaddrSel.   X2.U -> 2.U(5.W),
        ))
        out.gpr_ws  := op.gprWSel
        out.gpr_wen := op.gprWen
        out.csr_waddr := DontCare
        out.csr_wen := false.B

        val notIvd = Wire(Bool())
        notIvd := MuxLookup(op.limit, false.B)(Seq(
            Limit. NO.U -> true.B,
            Limit. RD.U -> out.gpr_waddr.orR,
            Limit.Imm.U -> out.imm.orR,
            Limit.RS1.U -> out.gpr_raddr1.orR,
        ))

        out.is_trap := false.B
        out.is_ivd  := !notIvd
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
        val dbgInst = Wire(UInt(32.W))
        
        val normalInstOp = Wire(new InstDecoderOut())
        InstDecode(inst, normalInstOp)

        val op = Wire(new InstDecoderOut())
        val ready = Wire(Bool())
        val valid = Wire(Bool())
        if (Config.Extension.C) {
            val saved = Reg(UInt(16.W))
            val left  = RegInit(false.B) // decode full inst anyway
            
            val inInstLow  = in.bits.inst(15, 0)
            val inInstHigh = in.bits.inst(31, 16)
            val pcAligned = !pc(1).asBool

            val s_empty :: s_saved :: Nil = Enum(2) // s_decode for decode left inst
            val state = RegInit(s_empty)

            val fullInst = Mux(state === s_empty, in.bits.inst, Cat(inInstLow, saved))
            val halfInst = Mux(state === s_empty, inInstLow, saved)
            
            val fullInstOp = Wire(new InstDecoderOut())
            val fullInstValid = !fullInstOp.is_ivd
            InstDecode(fullInst, fullInstOp)
            val halfInstOp = Wire(new InstDecoderOut())
            CInstDecode(halfInst, halfInstOp)

            state := Mux(predict_failed, s_empty, Mux(
                (in.valid || left) && !raw,
                Mux(
                    pcAligned,
                    MuxLookup(state, s_empty)(Seq(
                        s_empty -> Mux(fullInstValid, s_empty, s_saved),
                        s_saved -> Mux(fullInstValid, s_saved, s_empty)
                    )),
                    s_empty
                ),
                state
            ))

            op := Mux(fullInstValid, fullInstOp, halfInstOp)
            dbgInst := Mux(fullInstValid, fullInst, halfInst)
            
            snpc := in.bits.pc + MuxLookup(state, 0.U)(Seq(
                s_empty -> Mux(fullInstValid, 4.U(32.W), 2.U(32.W)),
                s_saved -> Mux(fullInstValid, 2.U(32.W), 0.U(32.W))
            ))
            pc := in.bits.pc - MuxLookup(state, 0.U)(Seq(
                s_empty -> 0.U,
                s_saved -> 2.U,
            ))
            when(out.valid && out.ready) {
                printf("0x%x 0x%x %d %d\n", pc, dbgInst, op.is_jmp, left)
            }
            // when(in.valid && out.ready) {
            //     printf("0x%x 0x%x\n", in.bits.pc, in.bits.inst)
            // }

            left := Mux(predict_failed || !pcAligned, false.B, state === s_saved && !fullInstValid)

            saved := Mux(!pcAligned, inInstHigh, inInstLow)

            valid := (left || in.valid) && pcAligned
            ready := (!(pcAligned && state === s_saved && !fullInstValid) || predict_failed) && out.ready 
        } else {
            op := normalInstOp
            ready := out.ready
            valid := in. valid

            if (Config.HasBTB) {
                snpc := pc + 4.U(32.W)
            } else {
                snpc := in.bits.snpc
            }
            dbgInst := inst
        }
        
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
        out.bits.gpr_waddr := Mux(op.gpr_wen, op.gpr_waddr, 0.U(5.W))
        out.bits.gpr_ws := op.gpr_ws
        
        // CSR
        out.bits.trap.is_trap := op.is_ivd || op.is_trap
        out.bits.trap.is_interrupt := false.B
        out.bits.trap.cause := Mux(op.is_ivd, 0x2.U(4.W), 0xb.U(4.W))
        
        out.bits.csr_wen := op.csr_wen
        out.bits.csr_waddr := op.csr_waddr

        // FENCE
        fence_i := op.fence_i

        // TAG
        out.bits.is_ivd := op.is_ivd
        out.bits.is_brk := op.is_brk

         in.ready := ready && !raw
        out.valid := valid && !raw && !predict_failed

        // when (in.valid) {
        //     printf(p"IDU: pc=0x${Hexadecimal(pc)} inst=0x${Hexadecimal(inst)} isBrk=${out.bits.is_brk}\n")
        // }

        // DEBUG
        out.bits.dbg.pc := pc
        out.bits.dbg.inst := dbgInst
    }

    def apply(bundle: IDUBundle) : Unit = {
        apply(
            in  = bundle.in ,
            out = bundle.out,
            gpr_raddr1 = bundle.gpr_raddr1,
            gpr_raddr2 = bundle.gpr_raddr2,
            gpr_rdata1 = bundle.gpr_rdata1,
            gpr_rdata2 = bundle.gpr_rdata2,
            gpr_ren1   = bundle.gpr_ren1,
            gpr_ren2   = bundle.gpr_ren2,
            csr_raddr  = bundle.csr_raddr,
            csr_rdata  = bundle.csr_rdata,
            csr_ren    = bundle.csr_ren,
            fence_i    = bundle.fence_i,
            raw        = bundle.raw,
            predict_failed = bundle.predict_failed
        )
    }
}

class IDU extends Module {
    val io = IO(new Bundle {
        val in = Flipped(Decoupled(new IFUMessage))
        val out = Decoupled(new IDUMessage)

        val gpr_raddr1 = Output(UInt(Config.GPRAddrWidth.W))
        val gpr_raddr2 = Output(UInt(Config.GPRAddrWidth.W))
        val gpr_rdata1 = Input (UInt(32.W))
        val gpr_rdata2 = Input (UInt(32.W))
        val gpr_ren1   = Output(Bool())
        val gpr_ren2   = Output(Bool())
        val csr_raddr  = Output(UInt(Config.CSRAddrWidth.W))
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
