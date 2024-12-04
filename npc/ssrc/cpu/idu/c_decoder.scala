package cpu.idu

import scala.collection.mutable.Map

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import cpu.idu.CInstDecoder.DecodeField.IsHitField
import scala.collection.mutable.ArrayBuffer
import cpu.idu.CSRAddrSel.Ins

object CImmType {
    val SL  = 0
    val SS  = 1
    val RLS = 2 
    val JI  = 3
    val B   = 4
    val LI  = 5
    val UI  = 6
    val AU  = 7
    val AS  = 8
    val I16 = 9
    val I4  = 10
}

object OverlapType {
    val NoOverlap = 0
    val JAL  = 1
    val JALR = 2 
}

object CGPRRaddr1Sel {
    val INST1 = 0; // inst[11:7]
    val INST2 = 1; // inst[ 9:7]
    val X2    = 2;
    val SP    = 2;
    val X0    = 3;
    val ZERO  = 3;
    val DontCare = 4;
}

object CGPRRaddr2Sel {
    val INST3 = 0; // inst[6:2]
    val INST4 = 1; // inst[4:2]
    val X0    = 2;
    val ZERO  = 2;
    val DontCare = 0;
}

object CGPRWaddrSel {
    val INST1 = 0; // inst[11:7]
    val INST2 = 1; // inst[ 9:7]
    val INST4 = 2; // inst[ 4:2]
    val X1    = 3;
    val RA    = 3;
    val X2    = 4;
    val SP    = 4;
    val DontCare = 0;
}

object Limit {
    val NO  = 0
    val RD  = 1
    val Imm = 2
    val RS1 = 3
}

import EXUTag.EXUTag

class CDecodeOPBundle extends Bundle {
    val immType = UInt(4.W)
    val aSel    = UInt(2.W)
    val bSel    = UInt(2.W)
    val cSel    = Bool()
    val gprRen1 = Bool()
    val gprRen2 = Bool()
    val gprRaddr1 = UInt(2.W)
    val gprRaddr2 = UInt(2.W)
    val gprWaddrSel = UInt(3.W)
    
    val func3   = UInt(3.W)
    val aluAdd  = Bool()
    val exuTag  = Bool()
    
    val isJmp    = Bool()
    val isBranch = Bool()
    
    val memRen  = Bool()
    val memWen  = Bool()

    val gprWen  = Bool()
    val gprWSel = UInt(2.W)
    val isBrk   = Bool()
    val isIvd   = Bool()
}

object CInstDecoder {
    object CInstType extends Enumeration {
        type CInstType = Value
        val SL, SS, RL, RS, J, JAL, JR, JALR, B, LI, LUI, IAU, IAS, I16, I4, MV, A, EB, IVD = Value
    }
    import CInstType.CInstType

    case class InstPattern(
        val pattern: BitPat, 
        val instType: CInstType, 
        val exuTag: EXUTag, 
        val func3: Int,
        val limit: Int,
    ) extends DecodePattern {
        def bitPat: BitPat = pattern
    }

    object DecodeField {
        object ImmTypeField extends DecodeField[InstPattern, UInt] {
            def name = "ImmType decode field"
            def chiselType: UInt = UInt(4.W)
            def genTable(op: InstPattern): BitPat = {
                val immType = op.instType match {
                    case CInstType. SL => CImmType. SL;
                    case CInstType. SS => CImmType. SS;
                    case CInstType. RL => CImmType.RLS;
                    case CInstType. RS => CImmType.RLS;
                    case CInstType.  J => CImmType. JI;
                    case CInstType.  B => CImmType.  B;
                    case CInstType. LI => CImmType. LI;
                    case CInstType.LUI => CImmType. UI;
                    case CInstType.IAU => CImmType. AU;
                    case CInstType.IAS => CImmType. AS;
                    case CInstType.I16 => CImmType.I16;
                    case CInstType.I4  => CImmType. I4;
                    case _             => CImmType.  B; //Dont Care
                }
                return BitPat(immType.U(4.W))
            }
        }

        object ASelField extends DecodeField[InstPattern, UInt] {
            def name = "ASel decode field"
            def chiselType: UInt = UInt(2.W)
            def genTable(op: InstPattern): BitPat = {
                 val sel = op.instType match {
                    case CInstType.  SL => ASel.GPR1;
                    case CInstType.  SS => ASel.GPR1;
                    case CInstType.  RL => ASel.GPR1;
                    case CInstType.  RS => ASel.GPR1;
                    case CInstType.   J => ASel.  PC;
                    case CInstType. JAL => ASel.  PC;
                    case CInstType.  JR => ASel.GPR1;
                    case CInstType.JALR => ASel.GPR1;
                    case CInstType.   B => ASel.  PC;
                    case CInstType.  LI => ASel.ZERO;
                    case CInstType. LUI => ASel.ZERO;
                    case CInstType. IAU => ASel.GPR1;
                    case CInstType. IAS => ASel.GPR1;
                    case CInstType. I16 => ASel.GPR1;
                    case CInstType.  I4 => ASel.GPR1;
                    case CInstType.  MV => ASel.GPR1;
                    case CInstType.   A => ASel.GPR1;
                    case _              => ASel.ZERO; // Dont Care
                }
                return BitPat(sel.U(2.W))
            }
        }

        object BSelField extends DecodeField[InstPattern, UInt] {
            def name = "BSel decode field"
            def chiselType: UInt = UInt(2.W)
            def genTable(op: InstPattern): BitPat = {
                val sel = op.instType match {
                    case CInstType.  SL => BSel.Imm;
                    case CInstType.  SS => BSel.Imm;
                    case CInstType.  RL => BSel.Imm;
                    case CInstType.  RS => BSel.Imm;
                    case CInstType.   J => BSel.Imm;
                    case CInstType. JAL => BSel.Imm;
                    case CInstType.  JR => BSel.Imm;
                    case CInstType.JALR => BSel.Imm;
                    case CInstType.  LI => BSel.Imm;
                    case CInstType. LUI => BSel.Imm;
                    case CInstType. IAU => BSel.Imm;
                    case CInstType. IAS => BSel.Imm;
                    case CInstType. I16 => BSel.Imm;
                    case CInstType.  I4 => BSel.Imm;
                    case CInstType.  MV => BSel.GPR2;
                    case CInstType.   A => BSel.GPR2;
                    case _              => BSel.DontCare; // DontCare
                }
                return BitPat(sel.U(2.W))
            }
        }

        object CSelField extends DecodeField[InstPattern, UInt] {
            def name = "CSel decode field"
            def chiselType: UInt = UInt(1.W)
            def genTable(op: InstPattern): BitPat = {
                val sel = Seq(
                    CInstType.JAL,
                    CInstType.JALR
                ).contains(op.instType)
                return BitPat(sel.B)
            }
        }

        object GPRRen1Field extends BoolDecodeField[InstPattern] {
            def name = "GPRRen1 decode field"
            def genTable(op: InstPattern): BitPat = {
                val ren = Seq(
                    CInstType.  SL,
                    CInstType.  SS,
                    CInstType.  RL,
                    CInstType.  RS,
                    CInstType.   B,
                    CInstType. IAU,
                    CInstType. IAS,
                    CInstType. I16,
                    CInstType.  I4,
                    CInstType.   A
                ).contains(op.instType)
                return BitPat(ren.B)
            }
        }

        object GPRRen2Field extends BoolDecodeField[InstPattern] {
            def name = "GPRRen2 decode field"
            def genTable(op: InstPattern): BitPat = {
                val ren = Seq(
                    CInstType.SS,
                    CInstType.RS,
                    CInstType.MV,
                    CInstType. A
                ).contains(op.instType)
                return BitPat(ren.B)
            }
        }

        object GPRRaddr1SelField extends DecodeField[InstPattern, UInt] {
            def name = "GPRRaddr1Sel decode field"
            def chiselType: UInt = UInt(2.W)
            def genTable(op: InstPattern): BitPat = {
                val sel = op.instType match {
                    case CInstType.  SL => CGPRRaddr1Sel.SP;
                    case CInstType.  SS => CGPRRaddr1Sel.SP; 
                    case CInstType.  RL => CGPRRaddr1Sel.INST2;
                    case CInstType.  RS => CGPRRaddr1Sel.INST2;
                    case CInstType.  JR => CGPRRaddr1Sel.INST1;
                    case CInstType.JALR => CGPRRaddr1Sel.INST1;
                    case CInstType.   B => CGPRRaddr1Sel.INST2;
                    case CInstType. IAU => CGPRRaddr1Sel.INST1;
                    case CInstType. I16 => CGPRRaddr1Sel.SP;
                    case CInstType.  I4 => CGPRRaddr1Sel.SP;
                    case CInstType. IAS => CGPRRaddr1Sel.INST2;
                    case CInstType.  MV => CGPRRaddr1Sel.ZERO;
                    case CInstType.   A => CGPRRaddr1Sel.INST2;
                    case _              => CGPRRaddr1Sel.DontCare;
                }
                if (sel == CGPRRaddr1Sel.DontCare) return BitPat.dontCare(2)
                return BitPat(sel.U(2.W))
            }
        }

        object GPRRaddr2SelField extends DecodeField[InstPattern, UInt] {
            def name = "GPRRaddr2Sel decode field"
            def chiselType: UInt = UInt(2.W)
            def genTable(op: InstPattern): BitPat = {
                val sel = op.instType match {
                    case CInstType.SS => CGPRRaddr2Sel.INST3;
                    case CInstType.RS => CGPRRaddr2Sel.INST4;
                    case CInstType. B => CGPRRaddr2Sel.ZERO;
                    case CInstType.MV => CGPRRaddr2Sel.INST3;
                    case CInstType. A => CGPRRaddr2Sel.INST4;
                    case _            => CGPRRaddr2Sel.DontCare; 
                }
                return BitPat(sel.U(2.W))
            }
        }

        object GPRWaddrSelField extends DecodeField[InstPattern, UInt] {
            def name = "GPRWaddrSel decode field"
            def chiselType: UInt = UInt(3.W)
            def genTable(op: InstPattern): BitPat = {
                val sel = op.instType match {
                    case CInstType.  SL => CGPRWaddrSel.INST1;
                    case CInstType.  RL => CGPRWaddrSel.INST4;
                    case CInstType. JAL => CGPRWaddrSel.RA;
                    case CInstType.JALR => CGPRWaddrSel.RA;
                    case CInstType.  LI => CGPRWaddrSel.INST1;
                    case CInstType. LUI => CGPRWaddrSel.INST1;
                    case CInstType. IAU => CGPRWaddrSel.INST1;
                    case CInstType. I16 => CGPRWaddrSel.SP;
                    case CInstType.  I4 => CGPRWaddrSel.INST4;
                    case CInstType. IAS => CGPRWaddrSel.INST2;
                    case CInstType.  MV => CGPRWaddrSel.INST1;
                    case CInstType.   A => CGPRWaddrSel.INST2;
                    case _              => CGPRWaddrSel.DontCare; 
                }
                return BitPat(sel.U(3.W))
            }
        }

        object Func3Field extends DecodeField[InstPattern, UInt] {
            def name = "Func3 decode field"
            def chiselType: UInt = UInt(3.W)
            def genTable(op: InstPattern): BitPat = {
                return BitPat(op.func3.U(3.W))
            }
        }

        object AluAddField extends BoolDecodeField[InstPattern] {
            def name = "AluAdd decode field"
            def genTable(op: InstPattern): BitPat = {
                val aluAdd = Seq(
                    CInstType.SL,
                    CInstType.SS,
                    CInstType.RL,
                    CInstType.RS,
                    CInstType. B,
                ).contains(op.instType)
                return BitPat(aluAdd.B)
            }
        }

        object EXUTagField extends BoolDecodeField[InstPattern] {
            def name = "EXUTag decode field"
            def genTable(op: InstPattern): BitPat = {
                if (op.exuTag == DontCare) return BitPat.dontCare(1)
                return BitPat((op.exuTag == EXUTag.T).B)
            }
        }

        object IsJmpField extends BoolDecodeField[InstPattern] {
            def name = "IsJmp decode field"
            def genTable(op: InstPattern): BitPat = {
                val isJmp = Seq(
                    CInstType.J,
                    CInstType.JR,
                    CInstType.JAL,
                    CInstType.JALR,
                ).contains(op.instType)
                return BitPat(isJmp.B)
            }
        }

        object IsBranchField extends BoolDecodeField[InstPattern] {
            def name = "IsBranch decode field"
            def genTable(op: InstPattern): BitPat = {
                val isBranch = op.instType == CInstType.B
                return BitPat(isBranch.B)
            }
        }

        object MemRenField extends BoolDecodeField[InstPattern] {
            def name = "MemRen decode field"
            def genTable(op: InstPattern): BitPat = {
                val memRen = Seq(
                    CInstType.SL,
                    CInstType.RL 
                ).contains(op.instType)
                return BitPat(memRen.B)
            }
        }

        object MemWenField extends BoolDecodeField[InstPattern] {
            def name = "MemWen decode field"
            def genTable(op: InstPattern): BitPat = {
                val memWen = Seq(
                    CInstType.SS,
                    CInstType.RS
                ).contains(op.instType)
                return BitPat(memWen.B)
            }
        }

        object GPRWenField extends BoolDecodeField[InstPattern] {
            def name = "GPRWen decode field"
            def genTable(op: InstPattern): BitPat = {
                val gprWen = Seq(
                    CInstType.SL,
                    CInstType.RL,
                    CInstType.JAL,
                    CInstType.JALR,
                    CInstType.LI,
                    CInstType.LUI,
                    CInstType.IAU,
                    CInstType.IAS,
                    CInstType.I16,
                    CInstType.I4,
                    CInstType.MV,
                    CInstType.A
                ).contains(op.instType)
                return BitPat(gprWen.B)
            }
        }

        object GPRWSelField extends DecodeField[InstPattern, UInt] {
            def name = "GPRWSel decode field"
            def chiselType: UInt = UInt(2.W)
            def genTable(op: InstPattern): BitPat = {
                val sel = op.instType match {
                    case CInstType.  SL => GPRWSel.MEM;
                    case CInstType.  RL => GPRWSel.MEM;
                    case CInstType. JAL => GPRWSel.SNPC;
                    case CInstType.JALR => GPRWSel.SNPC;
                    case CInstType.  LI => GPRWSel.EXU;
                    case CInstType. LUI => GPRWSel.EXU;
                    case CInstType. IAU => GPRWSel.EXU;
                    case CInstType. IAS => GPRWSel.EXU;
                    case CInstType. I16 => GPRWSel.EXU;
                    case CInstType.  I4 => GPRWSel.EXU;
                    case CInstType.  MV => GPRWSel.EXU;
                    case CInstType.   A => GPRWSel.EXU;
                    case _              => GPRWSel.EXU; // Dont care
                }
                return BitPat(sel.U(2.W))
            }
        }

        object LimitField extends DecodeField[InstPattern, UInt] {
            def name = "Limit decode field"
            def chiselType: UInt = UInt(2.W)
            def genTable(op: InstPattern): BitPat = {
                return BitPat(op.limit.U(2.W))
            }
        }

        object IsBrkField extends BoolDecodeField[InstPattern] {
            def name = "IsBrk decode field"
            def genTable(op: InstPattern): BitPat = {
                val isBrk = op.instType == CInstType.EB
                return BitPat(isBrk.B)
            }
            override def default: BitPat = BitPat(false.B)
        }

        object IsIvdField extends BoolDecodeField[InstPattern] {
            def name = "IsIvd decode field"
            def genTable(op: InstPattern): BitPat = {
                return BitPat((op.instType == CInstType.IVD).B)
            }
            override def default: BitPat = BitPat(true.B)
        }

        object IsHitField extends BoolDecodeField[InstPattern] {
            def name = "IsHit decode field"
            def genTable(op: InstPattern): BitPat = {
                return BitPat(true.B)
            }
            override def default: BitPat = BitPat(false.B)
        }
    }

    object Bits {
        // Load and Store
        val LWSP = BitPat("b010_?_?????_?????_10")
        val SWSP = BitPat("b110_??????_?????_10")
        val LW   = BitPat("b010_???_???_??_???_00")
        val SW   = BitPat("b110_???_???_??_???_00")

        // Control Transfer
        val J    = BitPat("b101_???????????_01")
        val JAL  = BitPat("b001_???????????_01")
        val JR   = BitPat("b1000_?????_00000_10")
        val JALR = BitPat("b1001_?????_00000_10")
        val BEQZ = BitPat("b110_???_???_?????_01")
        val BNEZ = BitPat("b111_???_???_?????_01")

        // Integer Computation
        val LI   = BitPat("b010_?_?????_?????_01")
        val LUI  = BitPat("b011_?_?????_?????_01")
        
        val ADDI = BitPat("b000_?_?????_?????_01")
        val ADDI4SPN = BitPat("b000_????????_???_00")
        val ADDI16SP = BitPat("b011_?_00010_?????_01")

        val SLLI = BitPat("b000_?_?????_?????_10")
        val SRLI = BitPat("b100_?_00_???_?????_01")
        val SRAI = BitPat("b100_?_01_???_?????_01")
        val ANDI = BitPat("b100_?_10_???_?????_01")

        // Integer Register-Register Operation
        val MV   = BitPat("b100_0_?????_?????_10")
        val ADD  = BitPat("b100_1_?????_?????_10")
        val SUB  = BitPat("b100_0_11_???_00_???_01")
        val XOR  = BitPat("b100_0_11_???_01_???_01")
        val OR   = BitPat("b100_0_11_???_10_???_01")
        val AND  = BitPat("b100_0_11_???_11_???_01")
        val NOP  = BitPat("b0000_0000_0000_0001")

        // Debug
        val EBREAK = BitPat("b1001000000000010")

        // Invalid
        val IVD_LWSP = BitPat("b010_?_00000_?????_10")
        val IVD_LI   = BitPat("b010_?_00000_?????_01")
        val IVD_LUI  = BitPat("b011_?_00000_?????_01")
        val IVD_ADDI = BitPat("b000_0_?????_00000_01")
        val IVD_ADDI16SP = BitPat("b011_0_00010_00000_01")
        val IVD_ADDI4SPN = BitPat("b000_00000000_???_00")
        val IVD_SLLI = BitPat("b000_0_?????_?????_10")
        val IVD_SRLI = BitPat("b100_0_00_???_?????_01")
        val IVD_SRAI = BitPat("b100_0_01_???_?????_01")
        val IVD_MV   = BitPat("b100_0_?????_00000_10")
        val IVD_ADD  = BitPat("b100_1_00000_?????_10")
    }

    val instTableArray = new ArrayBuffer[Seq[InstPattern]]

    // instTableArray += Seq(
    //     InstPattern(Bits.NOP,    CInstType.IAU, EXUTag.DontCare, Func3.ADD),
    //     InstPattern(Bits.EBREAK, CInstType.EB , EXUTag.DontCare, Func3.ADD),
    // )

    // instTableArray += Seq(
    //     InstPattern(Bits.IVD_LWSP, CInstType.IVD, EXUTag.DontCare, Func3.ADD),
    //     InstPattern(Bits.IVD_LI  , CInstType.IVD, EXUTag.DontCare, Func3.ADD),
    //     InstPattern(Bits.IVD_LUI , CInstType.IVD, EXUTag.DontCare, Func3.ADD),
    //     InstPattern(Bits.IVD_ADDI, CInstType.IVD, EXUTag.DontCare, Func3.ADD),
    //     InstPattern(Bits.IVD_ADDI16SP, CInstType.IVD, EXUTag.DontCare, Func3.ADD),
    //     InstPattern(Bits.IVD_ADDI4SPN, CInstType.IVD, EXUTag.DontCare, Func3.ADD),
    //     InstPattern(Bits.IVD_SLLI, CInstType.IVD, EXUTag.DontCare, Func3.SLL),
    //     InstPattern(Bits.IVD_SRLI, CInstType.IVD, EXUTag.T       , Func3.SR ),
    //     InstPattern(Bits.IVD_SRAI, CInstType.IVD, EXUTag.F       , Func3.SR ),
    //     InstPattern(Bits.IVD_MV  , CInstType.IVD, EXUTag.DontCare, Func3.ADD),
    //     InstPattern(Bits.IVD_ADD , CInstType.IVD, EXUTag.DontCare, Func3.ADD),
    // )

    instTableArray += Seq(
        InstPattern(Bits.JR      , CInstType.JR  , EXUTag.DontCare, Func3.ADD, Limit.RS1),
        InstPattern(Bits.JALR    , CInstType.JALR, EXUTag.DontCare, Func3.ADD, Limit. NO),
        InstPattern(Bits.ADDI16SP, CInstType.I16 , EXUTag.DontCare, Func3.ADD, Limit.Imm),
    )   

    instTableArray += Seq(
        InstPattern(Bits.LWSP, CInstType.SL, EXUTag.DontCare, Func3.W, Limit.RD),
        InstPattern(Bits.SWSP, CInstType.SS, EXUTag.DontCare, Func3.W, Limit.NO),
        InstPattern(Bits.LW  , CInstType.RL, EXUTag.DontCare, Func3.W, Limit.NO),
        InstPattern(Bits.SW  , CInstType.RS, EXUTag.DontCare, Func3.W, Limit.NO),

        InstPattern(Bits.J   , CInstType.J   , EXUTag.DontCare, Func3.ADD, Limit.NO),
        InstPattern(Bits.JAL , CInstType.JAL , EXUTag.DontCare, Func3.ADD, Limit.NO),

        InstPattern(Bits.BEQZ, CInstType.B, EXUTag.DontCare, Func3.EQ, Limit.NO),
        InstPattern(Bits.BNEZ, CInstType.B, EXUTag.DontCare, Func3.NE, Limit.NO),

        InstPattern(Bits.LI  , CInstType.LI , EXUTag.DontCare, Func3.ADD, Limit. RD),
        InstPattern(Bits.LUI , CInstType.LUI, EXUTag.DontCare, Func3.ADD, Limit.Imm),

        InstPattern(Bits.ADDI    , CInstType.IAU, EXUTag.F       , Func3.ADD, Limit. RD),
        InstPattern(Bits.ADDI4SPN, CInstType.I4 , EXUTag.DontCare, Func3.ADD, Limit.Imm),

        InstPattern(Bits.SLLI, CInstType.IAU, EXUTag.DontCare, Func3.SLL, Limit.NO),
        InstPattern(Bits.SRLI, CInstType.IAS, EXUTag.T       , Func3.SR , Limit.NO),
        InstPattern(Bits.SRAI, CInstType.IAS, EXUTag.F       , Func3.SR , Limit.NO),
        InstPattern(Bits.ANDI, CInstType.IAS, EXUTag.DontCare, Func3.AND, Limit.NO),

        InstPattern(Bits.MV , CInstType.MV, EXUTag.DontCare, Func3.ADD, Limit.RD),
        InstPattern(Bits.ADD, CInstType. A, EXUTag.DontCare, Func3.ADD, Limit.RD),
        InstPattern(Bits.SUB, CInstType. A, EXUTag.T       , Func3.ADD, Limit.NO),
        InstPattern(Bits.XOR, CInstType. A, EXUTag.DontCare, Func3.XOR, Limit.NO),
        InstPattern(Bits.OR , CInstType. A, EXUTag.DontCare, Func3.OR , Limit.NO),
        InstPattern(Bits.AND, CInstType. A, EXUTag.DontCare, Func3.AND, Limit.NO),
    )

    val decodeFieldSeq = Seq(
        DecodeField.ImmTypeField,
        DecodeField.ASelField,
        DecodeField.BSelField,
        DecodeField.CSelField,
        DecodeField.GPRRen1Field,
        DecodeField.GPRRen2Field,
        DecodeField.GPRRaddr1SelField,
        DecodeField.GPRRaddr2SelField,
        DecodeField.GPRWaddrSelField,
        DecodeField.Func3Field,
        DecodeField.AluAddField,
        DecodeField.EXUTagField,
        DecodeField.IsJmpField,
        DecodeField.IsBranchField,
        DecodeField.MemRenField,
        DecodeField.MemWenField,
        DecodeField.GPRWenField,
        DecodeField.GPRWSelField,
        DecodeField.IsBrkField,
        DecodeField.IsIvdField,
        DecodeField.IsHitField
    )

    val decodeTableArray = new ArrayBuffer[DecodeTable[InstPattern]]
    for (instTable <- instTableArray) {
        decodeTableArray += new DecodeTable(instTable, decodeFieldSeq)
    }

    def decode(inst: UInt, op: CDecodeOPBundle, table: DecodeTable[InstPattern]): Bool = {
        val decodeResult = table.decode(inst)
        op.immType := decodeResult(DecodeField.ImmTypeField)
        op.aSel    := decodeResult(DecodeField.ASelField)
        op.bSel    := decodeResult(DecodeField.BSelField)
        op.cSel    := decodeResult(DecodeField.CSelField)
        op.gprRen1 := decodeResult(DecodeField.GPRRen1Field)
        op.gprRen2 := decodeResult(DecodeField.GPRRen2Field)
        op.gprRaddr1 := decodeResult(DecodeField.GPRRaddr1SelField)
        op.gprRaddr2 := decodeResult(DecodeField.GPRRaddr2SelField)
        op.gprWaddrSel := decodeResult(DecodeField.GPRWaddrSelField)
        op.func3   := decodeResult(DecodeField.Func3Field)
        op.aluAdd  := decodeResult(DecodeField.AluAddField)
        op.exuTag  := decodeResult(DecodeField.EXUTagField)
        op.isJmp   := decodeResult(DecodeField.IsJmpField)
        op.isBranch := decodeResult(DecodeField.IsBranchField)
        op.memRen  := decodeResult(DecodeField.MemRenField)
        op.memWen  := decodeResult(DecodeField.MemWenField)
        op.gprWen  := decodeResult(DecodeField.GPRWenField)
        op.gprWSel := decodeResult(DecodeField.GPRWSelField)
        op.isBrk   := decodeResult(DecodeField.IsBrkField)
        op.isIvd   := decodeResult(DecodeField.IsIvdField)
        return decodeResult(DecodeField.IsHitField)
    }

    def decode_ebreak(op: CDecodeOPBundle) : Unit = {
        op.immType := DontCare
        op.aSel    := DontCare
        op.bSel    := DontCare
        op.cSel    := DontCare
        op.gprRen1 := DontCare
        op.gprRen2 := DontCare
        op.gprRaddr1 := DontCare
        op.gprRaddr2 := DontCare
        op.gprWaddrSel := DontCare
        op.func3   := DontCare
        op.aluAdd  := DontCare
        op.exuTag  := DontCare
        op.isJmp   := DontCare
        op.isBranch := DontCare
        op.memRen  := DontCare
        op.memWen  := DontCare
        op.gprWen  := DontCare
        op.gprWSel := DontCare
        op.isBrk   := true.B
        op.isIvd   := false.B
    }

    def decode(inst: UInt, op: CDecodeOPBundle): Unit = {
        val opArray  = new ArrayBuffer[CDecodeOPBundle]
        val hitArray = new ArrayBuffer[Bool]
        for (i <- 0 until decodeTableArray.length) {
            val lop = Wire(new CDecodeOPBundle)
            val hit = decode(inst, lop, decodeTableArray(i))
            opArray  += lop
            hitArray += hit
        }
        val isEBreak = (inst == Bits.EBREAK.value.U(16.W)).B
        val ebreakOP = Wire(new CDecodeOPBundle)
        decode_ebreak(ebreakOP)
        op := Mux(isEBreak, Mux(hitArray(0), opArray(0), opArray(1)), ebreakOP)
    }
}
