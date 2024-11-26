package cpu.idu

import scala.collection.mutable.Map

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import cpu.reg.GPRWSel

object CInstType extends Enumeration {
    type CInstType = Value
    val SL, SS, RL, RS, J, JAL, JR, JALR, B, LI, LUI, IAU, IAS, I16, I4, R, A, EB, IVD = Value
}

object CImmType extends Enumeration {
    type CImmType = Value
    val SL, SS, RLS, JI, B, LI, UI, AU, AS, I16, I4 = Value
}

object CGPRRaddr1Sel extends {
    val INST1 = 0;
    val INST2 = 1;
    val X2    = 2;
    val SP    = 2;
    val DontCare = 0;
}

object CGPRRaddr2Sel extends Enumeration {
    val INST1 = 0;
    val INST2 = 1;
    val X0    = 2;
    val ZERO  = 2;
    val DontCare = 0;
}

object CGPRWaddrSel extends Enumeration {
    val INST1 = 0;
    val INST2 = 1;
    val INST3 = 2;
    val X1    = 3;
    val RA    = 3;
    val X2    = 4;
    val SP    = 4;
    val DontCare = 0;
}

import CInstType.CInstType
import EXUTag.EXUTag

object CExtensionEncode {
    class Tag(s: Int, l: Int, i: Int) {
        val start = s
        val length = l
        val mask = (1 << l) - 1
        val index = i
    }

    val tags: Map[String, Tag] = Map()
    var current = 0
    var count = 0

    def add_tag(name: String, length: Int) = {
        tags += (name -> new Tag(current, length, count))
        current += length
        count += 1
    }

    // IDU
    add_tag("ImmType",  4)
    add_tag("ASel",     2)
    add_tag("BSel",     2)
    add_tag("CSel",     1)
    add_tag("GPRRen1",  1)
    add_tag("GPRRen2",  1)
    add_tag("GPRRaddr1",2)
    add_tag("GPRRaddr2",2)
    add_tag("GPRWaddr", 3)
    
    // EXU
    add_tag("Func3",    1)
    add_tag("AluAdd",   1) // for load, save, jal, jalr
    add_tag("EXUTag",   1) // for sub or unsigned

    // Jump
    add_tag("DNPCSel",  1)
    add_tag("IsJmp",    1) // no condition jump
    add_tag("IsBranch", 1)

    // LSU
    add_tag("MemRen",   1)
    add_tag("MemWen",   1)
    
    // WBU
    add_tag("GPRWen",   1)
    add_tag("GPRWSel",  2)
    add_tag("CSRWen",   1)
    add_tag("IsBrk",    1)
    add_tag("IsIvd",    1)

    def toInt(boolValue: Boolean): Int = if(boolValue) 1 else 0

    def encode(instType: CInstType, exuTag: EXUTag, func3: Int) : BitPat = {
        val m: Map[String, Int] = Map()

        val immType = instType match {
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
        m += ("ImmType" -> immType.id)

        val aSel = instType match {
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
            case CInstType.   R => ASel.GPR1;
            case CInstType.   A => ASel.GPR1;
            case _              => ASel.ZERO; // Dont Care
        }
        m += ("ASel" -> aSel)

        val bSel = instType match {
            case CInstType.  SL => BSel.Imm;
            case CInstType.  SS => BSel.Imm;
            case CInstType.  RL => BSel.Imm;
            case CInstType.  RS => BSel.Imm;
            case CInstType.   J => BSel.Imm;
            case CInstType. JAL => BSel.Imm;
            case CInstType.  JR => BSel.Imm;
            case CInstType.JALR => BSel.Imm;
            case CInstType.   B => BSel.Imm;
            case CInstType.  LI => BSel.Imm;
            case CInstType. LUI => BSel.Imm;
            case CInstType. IAU => BSel.Imm;
            case CInstType. IAS => BSel.Imm;
            case CInstType. I16 => BSel.Imm;
            case CInstType.  I4 => BSel.Imm;
            case CInstType.   R => BSel.GPR2;
            case CInstType.   A => BSel.GPR2;
            case _              => BSel.DontCare; // DontCare
        }
        m += ("BSel" -> bSel)

        val cSel = Seq(
            CInstType.JAL,
            CInstType.JALR
        ).contains(instType)
        m += ("CSel" -> toInt(cSel))

        val gprRen1 = aSel == ASel.GPR1 || instType == CInstType.B
        m += ("GPRRen1" -> toInt(gprRen1))

        val gprRen2 = bSel == BSel.GPR2 || Seq(CInstType.SS, CInstType.RS).contains(instType)
        m += ("GPRRen2" -> toInt(gprRen2))

        val gprRaddr1Sel = instType match {
            case CInstType.  SL => CGPRRaddr1Sel.SP;
            case CInstType.  SS => CGPRRaddr1Sel.SP; 
            case CInstType.  RL => CGPRRaddr1Sel.INST2;
            case CInstType.  RS => CGPRRaddr1Sel.INST2;
            case CInstType.  JR => CGPRRaddr1Sel.INST1;
            case CInstType.JALR => CGPRRaddr1Sel.INST1;
            case CInstType.   B => CGPRRaddr1Sel.INST1;
            case CInstType. IAU => CGPRRaddr1Sel.INST1;
            case CInstType. I16 => CGPRRaddr1Sel.SP;
            case CInstType.  I4 => CGPRRaddr1Sel.SP;
            case CInstType. IAS => CGPRRaddr1Sel.INST2;
            case CInstType.   R => CGPRRaddr1Sel.INST1;
            case CInstType.   A => CGPRRaddr1Sel.INST2;
            case _              => CGPRRaddr1Sel.DontCare;
        }
        m += ("GPRRaddr1" -> gprRaddr1Sel)

        val gprRaddr2Sel = instType match {
            case CInstType.  SS => CGPRRaddr2Sel.INST1;
            case CInstType.  RS => CGPRRaddr2Sel.INST2;
            case CInstType.  JR => CGPRRaddr2Sel.INST1;
            case CInstType.JALR => CGPRRaddr2Sel.INST1;
            case CInstType.   B => CGPRRaddr2Sel.ZERO;
            case CInstType.   R => CGPRRaddr2Sel.INST1;
            case CInstType.   A => CGPRRaddr2Sel.INST1;
            case _              => CGPRRaddr2Sel.DontCare; 
        }
        m += ("GPRaddr2" -> gprRaddr2Sel)

        val gprWaddrSel = instType match {
            case CInstType.  SL => CGPRWaddrSel.INST1;
            case CInstType.  RL => CGPRWaddrSel.INST1;
            case CInstType. JAL => CGPRWaddrSel.RA;
            case CInstType.JALR => CGPRWaddrSel.RA;
            case CInstType.  LI => CGPRWaddrSel.INST1;
            case CInstType. LUI => CGPRWaddrSel.INST1;
            case CInstType. IAU => CGPRWaddrSel.INST1;
            case CInstType. I16 => CGPRWaddrSel.SP;
            case CInstType.  I4 => CGPRWaddrSel.INST2;
            case CInstType. IAS => CGPRWaddrSel.INST3;
            case CInstType.   R => CGPRWaddrSel.INST1;
            case CInstType.   A => CGPRWaddrSel.INST3;
            case _              => CGPRWaddrSel.DontCare; 
        }
        m += ("GPRWaddr" -> gprWaddrSel)

        m += ("Func3" -> func3)

        val aluAdd = Seq(
            CInstType.SL,
            CInstType.SS,
            CInstType.RL,
            CInstType.RS,
            CInstType. B,
        ).contains(instType)
        m += ("AluAdd" -> toInt(aluAdd))

        m += ("EXUTag" -> toInt(exuTag == EXUTag.T))

        val isJmp = Seq(
            CInstType.J,
            CInstType.JR,
            CInstType.JAL,
            CInstType.JALR,
        ).contains(instType)
        m += ("IsJmp" -> toInt(isJmp))

        val isBranch = instType == CInstType.B
        m += ("IsBranch" -> toInt(isBranch))

        val memRen = Seq(
            CInstType.SL,
            CInstType.RL 
        ).contains(instType)
        m += ("MemRen" -> toInt(memRen))

        val memWen = Seq(
            CInstType.SS,
            CInstType.RS
        ).contains(instType)
        m += ("MemWen" -> toInt(memWen))
        
        val gprWen = Seq(
            CInstType.SL,
            CInstType.RL,
            CInstType.JAL,
            CInstType.JALR,
            CInstType.LI,
            CInstType.LUI,
            CInstType.IAU,
            CInstType.IAU,
            CInstType.I16,
            CInstType.I4,
            CInstType.R,
            CInstType.A
        ).contains(instType)
        m += ("GPRWen" -> toInt(gprWen))

        val gprWSel = instType match {
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
            case CInstType.   R => GPRWSel.EXU;
            case CInstType.   A => GPRWSel.EXU;
            case _              => GPRWSel.EXU; // Dont care
        }
        m += ("GPRWSel" -> gprWSel)

        val isBrk = toInt(instType == CInstType.EB)
        m += ("IsBrk" -> isBrk)

        val isIvd = toInt(instType == CInstType.IVD)
        m += ("IsIvd" -> isIvd)
        
        return Encode.gen_bitpat(m)
    }
}

import CExtensionEncode.encode
object CExtensionDecoder {
    // Load and Store
    val LWSP = BitPat("b010_?_?????_?????_10")
    val SWSP = BitPat("b110_??????_?????_10")
    val LW   = BitPat("010_???_???_??_???_00")
    val SW   = BitPat("110_???_???_??_???_00")

    // Control Transfer
    val J    = BitPat("b101_???????????_01")
    val JAL  = BitPat("b001_???????????_01")
    val JR   = BitPat("b1000_?????_00000_10")
    val JALR = BitPat("b1001_00000_00000_10")
    val BEQZ = BitPat("b110_???_???_?????_01")
    val BNEZ = BitPat("b111_???_???_?????_01")

    // Integer Computation
    val LI   = BitPat("b001_?_?????_?????_01")
    val LUI  = BitPat("b011_?_?????_?????_01")
    
    val ADDI = BitPat("b000_?_?????_?????_01")
    val ADDI4SPN = BitPat("b000_????????_???_00")
    val ADDI16SP = BitPat("b011_?_?????_?????_01")

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

    // debug
    val BREAK = BitPat("b1001000000000010")

    val truthTable = TruthTable(
        Map(
            LWSP    -> encode(CInstType. SL, EXUTag.DontCare, Func3.W),
            SWSP    -> encode(CInstType. SS, EXUTag.DontCare, Func3.W),
            LW      -> encode(CInstType. RL, EXUTag.DontCare, Func3.W),
            SW      -> encode(CInstType. RS, EXUTag.DontCare, Func3.W),

            J       -> encode(CInstType.J   , EXUTag.DontCare, Func3.ADD),
            JR      -> encode(CInstType.JR  , EXUTag.DontCare, Func3.ADD),
            JAL     -> encode(CInstType.JAL , EXUTag.DontCare, Func3.ADD),
            JALR    -> encode(CInstType.JALR, EXUTag.DontCare, Func3.ADD),
            
            BEQZ    -> encode(CInstType.B, EXUTag.DontCare, Func3.EQ),
            BNEZ    -> encode(CInstType.B, EXUTag.DontCare, Func3.NE),

            LI      -> encode(CInstType.LI , EXUTag.DontCare, Func3.ADD),
            LUI     -> encode(CInstType.LUI, EXUTag.DontCare, Func3.ADD),

            ADDI     -> encode(CInstType.IAU, EXUTag.T,        Func3.ADD),
            ADDI16SP -> encode(CInstType.I16, EXUTag.DontCare, Func3.ADD),
            ADDI4SPN -> encode(CInstType.I4 , EXUTag.DontCare, Func3.ADD),
            
            SLLI    -> encode(CInstType.IAU, EXUTag.DontCare, Func3.SLL),
            SRLI    -> encode(CInstType.IAS, EXUTag.F       , Func3.SR ),
            SRAI    -> encode(CInstType.IAS, EXUTag.T       , Func3.SR ),
            ANDI    -> encode(CInstType.IAS, EXUTag.DontCare, Func3.AND),

            MV      -> encode(CInstType.R, EXUTag.DontCare, Func3.ADD),
            ADD     -> encode(CInstType.R, EXUTag.DontCare, Func3.ADD),

            AND     -> encode(CInstType.A, EXUTag.DontCare, Func3.AND),
            OR      -> encode(CInstType.A, EXUTag.DontCare, Func3.OR ),
            XOR     -> encode(CInstType.A, EXUTag.DontCare, Func3.XOR),
            SUB     -> encode(CInstType.A, EXUTag.T       , Func3.ADD),

            BREAK   -> encode(CInstType.EB, EXUTag.DontCare, Func3.ADD)
        ),
        encode(CInstType.IVD, EXUTag.DontCare, Func3.ADD)
    )
}