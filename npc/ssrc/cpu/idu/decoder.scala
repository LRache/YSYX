package cpu.idu

import scala.collection.mutable.Map

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import scala.collection.mutable.ArrayBuffer
import cpu.Config

object Func3 {
    val ADD  = 0
    val SLL  = 1
    val SLT  = 2
    val SLTU = 3
    val XOR  = 4
    val SR   = 5
    val OR   = 6
    val AND  = 7

    val BS = 0
    val HS = 1
    val W  = 2
    val BU = 4
    val HU = 5

    val EQ  = 0
    val NE  = 1
    val LT  = 4
    val GE  = 5
    val LTU = 6
    val GEU = 7
}

object Func3UInt {
    val ADD  = Func3. ADD.U
    val SLL  = Func3. SLL.U
    val SLT  = Func3. SLT.U
    val SLTU = Func3.SLTU.U
    val XOR  = Func3. XOR.U
    val SR   = Func3.  SR.U
    val OR   = Func3.  OR.U
    val AND  = Func3. AND.U
}

object EXUTag extends Enumeration {
    type EXUTag = Value
    val F, T, DontCare = Value
}
import EXUTag.EXUTag

object ImmType {
    val I  = 0
    val IU = 1
    val S  = 2
    val B  = 3
    val U  = 4
    val J  = 5
    val C  = 6
    val DontCare = 0
}

object ASel {
    val PC   = 0
    val GPR1 = 1
    val CSR  = 2
    val ZERO = 3
}

object BSel {
    val Imm  = 0
    val GPR1 = 1
    val GPR2 = 2
    val CSR  = 3   
    val DontCare = 0
}

object CSRAddrSel {
    val Ins = 0
    val VEC = 1
    val EPC = 2
    val DontCare = 0
}

object GPRWSel {
    val SNPC = 0b00
    val CSR  = 0b01
    val EXU  = 0b10
    val MEM  = 0b11
    val DontCare = 0
}

class DecodeOPBundle extends Bundle {
    val immType = UInt(3.W)
    val aSel    = UInt(2.W)
    val bSel    = UInt(2.W)
    val cSel    = Bool()
    val dSel    = Bool()
    val gprRen1 = Bool()
    val gprRen2 = Bool()
    val csrRAddrSel = UInt(2.W)
    val csrRen  = Bool()
    val fenceI  = Bool()
    val isTrap  = Bool()

    // EXU
    val aluAdd = Bool()
    val exuTag = Bool()

    // Jump
    val dnpcSel  = Bool()
    val isJmp    = Bool()
    val isBranch = Bool()
    
    // LSU
    val memRen = Bool()
    val memWen = Bool()

    // WBU
    val gprWen  = Bool()
    val gprWSel = UInt(2.W)
    val csrWen  = Bool()
    val isBrk   = Bool()
    val isIvd   = Bool()
}

object InstDecoder {
    object InstType extends Enumeration {
        type InstType = Value
        val R, RC, IC, IA, IJ, IU, L, S, B, J, UL, UA, CR, CI, EB, EC, MR, FI, IVD = Value
        // RC -> for xor, slt, sltu
        // IC -> for xori, slti
        // IA -> normal I Arithmetic, like addi
        // IJ -> only jalr jump and link reg
        // IU -> only for sltiu
        // L  -> for load
        // S  -> for save
        // B  -> for branch
        // J  -> only jal
        // UL -> only lui
        // UA -> only auipc
        // C  -> for csr
        // EC -> for ecall
        // MR -> for mret
        // EB -> for ebreak
        // FI -> for fence i
        // IVD -> invalid
    }
    
    import InstType.InstType
    case class InstPattern(val pattern: BitPat, val instType: InstType, val exuTag: EXUTag) extends DecodePattern {
        def bitPat: BitPat = {
            pattern
        }
    }
    
    object DecodeField {
        object ImmTypeField extends DecodeField[InstPattern, UInt] {
            def name = "ImmType decode field"
            def chiselType = UInt(3.W)
            def genTable(op: InstPattern) : BitPat = {
                val immType = op.instType match {
                    case InstType.IA => ImmType. I
                    case InstType.IJ => ImmType. I
                    case InstType. L => ImmType. I
                    case InstType.IU => ImmType.IU
                    case InstType. S => ImmType. S
                    case InstType. B => ImmType. B
                    case InstType.UA => ImmType. U
                    case InstType.UL => ImmType. U
                    case InstType. J => ImmType. J
                    case InstType.CI => ImmType. C
                    case _ => ImmType.DontCare
                }
                return BitPat(immType.U(3.W))
            }
        }

        object ASelField extends DecodeField[InstPattern, UInt] {
            def name = "ASel decode field"
            def chiselType = UInt(2.W)
            def genTable(op: InstPattern): BitPat = {
                val sel = op.instType match {
                    case InstType. R => ASel.GPR1
                    case InstType.IA => ASel.GPR1
                    case InstType.IJ => ASel.GPR1
                    case InstType.IU => ASel.GPR1
                    case InstType. L => ASel.GPR1
                    case InstType. S => ASel.GPR1
                    case InstType. B => ASel.  PC
                    case InstType. J => ASel.  PC
                    case InstType.UA => ASel.  PC
                    case InstType.CR => ASel. CSR
                    case InstType.CI => ASel. CSR
                    case InstType.EC => ASel.  PC // mepc = pc, pc = mtvec
                    case InstType.FI => ASel.  PC
                    case _ => ASel.ZERO
                }
                return BitPat(sel.U(2.W))
            }
            override def default: BitPat = BitPat(ASel.PC.U(2.W)) // Invalid Type
        }

        object BSelField extends DecodeField[InstPattern, UInt] {
            def name = "BSel decode field"
            def chiselType = UInt(2.W)
            def genTable(op: InstPattern): BitPat = {
                val sel = op.instType match {
                    case InstType. R => BSel.GPR2
                    case InstType.IA => BSel. Imm
                    case InstType.IJ => BSel. Imm
                    case InstType.IU => BSel. Imm
                    case InstType. L => BSel. Imm
                    case InstType. S => BSel. Imm
                    case InstType. J => BSel. Imm
                    case InstType. B => BSel. Imm
                    case InstType.UL => BSel. Imm
                    case InstType.UA => BSel. Imm
                    case InstType.CR => BSel.GPR1
                    case InstType.CI => BSel. Imm
                    case InstType.EC => BSel. CSR
                    case InstType.MR => BSel. CSR
                    case _ => BSel.DontCare
                }
                return BitPat(sel.U(2.W))
            }
            override def default: BitPat = BitPat(BSel.CSR.U(2.W)) // Invalid Type
        }

        object CSelField extends BoolDecodeField[InstPattern] {
            def name = "CSel decode field"
            def genTable(op: InstPattern): BitPat = {
                val sel = Seq(
                    InstType.IJ,
                    InstType. J
                ).contains(op.instType)
                return BitPat(sel.B)
            }
        }

        object DSelField extends BoolDecodeField[InstPattern] {
            def name = "DSel decode field"
            def genTable(op: InstPattern): BitPat = {
                val sel = Seq(
                    InstType.IA, 
                    InstType.IU
                ).contains(op.instType)
                return BitPat(sel.B)
            }
        }

        object GPRRen1Field extends BoolDecodeField[InstPattern] {
            def name = "GPRRen1 decode field"
            def genTable(op: InstPattern): BitPat = {
                val ren = Seq(
                    InstType. R,
                    InstType.IA,
                    InstType.IJ,
                    InstType.IU,
                    InstType. L,
                    InstType. S,
                    InstType. B,
                    InstType.CR,
                ).contains(op.instType)
                return BitPat(ren.B)
            }
        }

        object GPRRen2Field extends BoolDecodeField[InstPattern] {
            def name = "GPRRen2 decode field"
            def genTable(op: InstPattern): BitPat = {
                val ren = Seq(
                    InstType. R,
                    InstType. B,
                    InstType. S,
                ).contains(op.instType)
                return BitPat(ren.B)
            }
        }

        object CSRRAddrSelField extends DecodeField[InstPattern, UInt] {
            def name = "CSRRAddrSel decode field"
            def chiselType = UInt(2.W)
            def genTable(op: InstPattern): BitPat = {
                val isTrap = Seq(
                    InstType.EC, 
                    InstType.IVD
                ).contains(op.instType)
                val sel = if(isTrap) CSRAddrSel.VEC else op.instType match {
                    case InstType.MR => CSRAddrSel.EPC
                    case InstType.CR => CSRAddrSel.Ins
                    case InstType.CI => CSRAddrSel.Ins
                    case _ => CSRAddrSel.DontCare
                }
                return BitPat(sel.U(2.W))
            }
            override def default: BitPat = BitPat(CSRAddrSel.VEC.U(2.W))
        }

        object CSRRenField extends BoolDecodeField[InstPattern] {
            def name = "CSRRen decode field"
            def genTable(op: InstPattern): BitPat = {
                val ren = Seq(
                    InstType. CR,
                    InstType. CI,
                    InstType. EC,
                    InstType. MR,
                    InstType.IVD,
                ).contains(op.instType)
                return BitPat(ren.B)
            }
            override def default: BitPat = BitPat(true.B)
        }

        object IsTrapField extends BoolDecodeField[InstPattern] {
            def name = "IsTrap decode field"
            def genTable(op: InstPattern): BitPat = {
                val isTrap = op.instType == InstType.EC
                return BitPat(isTrap.B)
            }
        }

        object FenceIField extends BoolDecodeField[InstPattern] {
            def name = "FenceI decode field"
            def genTable(op: InstPattern): BitPat = {
                val isFenceI = op.instType == InstType.FI
                return BitPat(isFenceI.B)
            }
        }

        object AluAddField extends BoolDecodeField[InstPattern] {
            def name = "AluAdd decode field"
            def genTable(op: InstPattern): BitPat = {
                val aluAdd = Seq(
                    InstType. L, // load
                    InstType. S, // save
                    InstType.UA, // auipc
                    InstType. B, // branch
                    InstType. J, // jal
                    InstType.UL, // lui
                    InstType.MR  // mret
                ).contains(op.instType)
                return BitPat(aluAdd.B)
            }
        }

        object EXUTagField extends BoolDecodeField[InstPattern] {
            def name = "EXUTag decode field"
            def genTable(op: InstPattern): BitPat = {
                val exuTag = op.exuTag == EXUTag.T
                return BitPat(exuTag.B)
            }
        }

        object DNPCSelField extends BoolDecodeField[InstPattern] {
            def name = "DNPCSel decode field"
            def genTable(op: InstPattern): BitPat = {
                val dnpcSel = Seq(
                    InstType.EC,
                    InstType.MR,
                ).contains(op.instType)
                return BitPat(dnpcSel.B)
            }
        }

        object IsJmpField extends BoolDecodeField[InstPattern] {
            def name = "IsJmp decode field"
            def genTable(op: InstPattern): BitPat = {
                val isJmp = Seq(
                    InstType. J,
                    InstType.IJ,
                    InstType.MR
                ).contains(op.instType)
                return BitPat(isJmp.B)
            }
        }

        object IsBranchField extends BoolDecodeField[InstPattern] {
            def name = "IsBranch decode field"
            def genTable(op: InstPattern): BitPat = {
                val isBranch = op.instType == InstType.B
                return BitPat(isBranch.B)
            }
        }

        object MemRenField extends BoolDecodeField[InstPattern] {
            def name = "MemRen decode field"
            def genTable(op: InstPattern): BitPat = {
                val memRen = op.instType == InstType.L
                return BitPat(memRen.B)
            }
        }

        object MemWenField extends BoolDecodeField[InstPattern] {
            def name = "MemWen decode field"
            def genTable(op: InstPattern): BitPat = {
                val memWen = op.instType == InstType.S
                return BitPat(memWen.B)
            }
        }

        object GPRWenField extends BoolDecodeField[InstPattern] {
            def name = "GPRWen decode field"
            def genTable(op: InstPattern): BitPat = {
                val gprWen = Seq(
                    InstType. R,
                    InstType.IA,
                    InstType.IU,
                    InstType. L,
                    InstType.IJ,
                    InstType. J,
                    InstType.UL,
                    InstType.UA,
                    InstType.CR,
                    InstType.CI
                ).contains(op.instType)
                return BitPat(gprWen.B)
            }
        }

        object GPRWSelField extends DecodeField[InstPattern, UInt] {
            def name = "GPRWSel decode field"
            def chiselType = UInt(2.W)
            def genTable(op: InstPattern): BitPat = {
                val gprWSel = op.instType match {
                    case InstType. R => GPRWSel. EXU
                    case InstType.IA => GPRWSel. EXU
                    case InstType.IU => GPRWSel. EXU
                    case InstType. L => GPRWSel. MEM
                    case InstType.IJ => GPRWSel.SNPC
                    case InstType. J => GPRWSel.SNPC
                    case InstType.UL => GPRWSel. EXU
                    case InstType.UA => GPRWSel. EXU
                    case InstType.CR => GPRWSel. CSR
                    case InstType.CI => GPRWSel. CSR
                    case _ => GPRWSel.EXU
                }
                return BitPat(gprWSel.U(2.W))
            }
        }

        object CSRWenField extends BoolDecodeField[InstPattern] {
            def name = "CSRWen decode field"
            def genTable(op: InstPattern): BitPat = {
                val csrWen = Seq(InstType.CR, InstType.CI).contains(op.instType)
                return BitPat(csrWen.B)
            }
        }

        object IsBrkField extends BoolDecodeField[InstPattern] {
            def name = "IsBrk decode field"
            def genTable(op: InstPattern): BitPat = {
                val isBrk = op.instType == InstType.EB
                return BitPat(isBrk.B)
            }
        }

        object IsIvdField extends BoolDecodeField[InstPattern] {
            def name = "IsIvd decode field"
            def genTable(op: InstPattern): BitPat = {
                return BitPat(false.B)
            }
            override def default: BitPat = BitPat(true.B)
        }
    }

    object Bits {
        val ADD     = BitPat("b0000000_?????_?????_000_?????_011_0011")
        val SUB     = BitPat("b0100000_?????_?????_000_?????_011_0011")
        val AND     = BitPat("b0000000_?????_?????_111_?????_011_0011")
        val OR      = BitPat("b0000000_?????_?????_110_?????_011_0011")
        val XOR     = BitPat("b0000000_?????_?????_100_?????_011_0011")
        val SLL     = BitPat("b0000000_?????_?????_001_?????_011_0011")
        val SRL     = BitPat("b0000000_?????_?????_101_?????_011_0011")
        val SRA     = BitPat("b0100000_?????_?????_101_?????_011_0011")
        val SLT     = BitPat("b0000000_?????_?????_010_?????_011_0011")
        val SLTU    = BitPat("b0000000_?????_?????_011_?????_011_0011")

        val ADDI    = BitPat("b????????????_?????_000_?????_001_0011")
        val ANDI    = BitPat("b????????????_?????_111_?????_001_0011")
        val ORI     = BitPat("b????????????_?????_110_?????_001_0011")
        val XORI    = BitPat("b????????????_?????_100_?????_001_0011")
        val SLLI    = BitPat("b????????????_?????_001_?????_001_0011")
        val SRLI    = BitPat("b0000000_?????_?????_101_?????_001_0011")
        val SRAI    = BitPat("b0100000_?????_?????_101_?????_001_0011")
        val SLTI    = BitPat("b????????????_?????_010_?????_001_0011")
        val SLTIU   = BitPat("b????????????_?????_011_?????_001_0011")

        val JALR    = BitPat("b????????????_?????_000_?????_110_0111")

        val LB      = BitPat("b????????????_?????_000_?????_000_0011")
        val LH      = BitPat("b????????????_?????_001_?????_000_0011")
        val LW      = BitPat("b????????????_?????_010_?????_000_0011")
        val LBU     = BitPat("b????????????_?????_100_?????_000_0011")
        val LHU     = BitPat("b????????????_?????_101_?????_000_0011")
        val LOAD    = BitPat("b?????????????????_???_?????_000_0011")

        val SB      = BitPat("b???????_?????_?????_000_?????_010_0011")
        val SH      = BitPat("b???????_?????_?????_001_?????_010_0011")
        val SW      = BitPat("b???????_?????_?????_010_?????_010_0011")
        val SAVE    = BitPat("b?????????????????_???_?????_010_0011")

        val JAL     = BitPat("b????????????????????_?????_110_1111")

        val BEQ     = BitPat("b???????_?????_?????_000_?????_110_0011")
        val BGE     = BitPat("b???????_?????_?????_101_?????_110_0011")
        val BGEU    = BitPat("b???????_?????_?????_111_?????_110_0011")
        val BLT     = BitPat("b???????_?????_?????_100_?????_110_0011")
        val BLTU    = BitPat("b???????_?????_?????_110_?????_110_0011")
        val BNE     = BitPat("b???????_?????_?????_001_?????_110_0011")
        val BRANCH  = BitPat("b???????_?????_?????_???_?????_110_0011")

        val AUIPC   = BitPat("b????????????????????_?????_001_0111")
        val LUI     = BitPat("b????????????????????_?????_011_0111")

        val FENCE_I = BitPat("b?????????????????_001_?????_000_1111")

        val CSRRW   = BitPat("b????????????_?????_001_?????_111_0011")
        val CSRRS   = BitPat("b????????????_?????_010_?????_111_0011")
        val CSRRC   = BitPat("b????????????_?????_011_?????_111_0011")
        val CSRRWI  = BitPat("b????????????_?????_101_?????_111_0011")
        val CSRRSI  = BitPat("b????????????_?????_110_?????_111_0011")
        val CSRRCI  = BitPat("b????????????_?????_111_?????_111_0011")
        
        val EBREAK  = BitPat("b00000000000100000000_00000_111_0011")
        val ECALL   = BitPat("b00000000000000000000_00000_111_0011")
        val MRET    = BitPat("b00110000001000000000_00000_111_0011")
    }

    val instTable = Seq(
        InstPattern(Bits.ADD,  InstType. R, EXUTag.F),
        InstPattern(Bits.SUB,  InstType. R, EXUTag.T),
        InstPattern(Bits.AND,  InstType. R, EXUTag.F),
        InstPattern(Bits.OR,   InstType. R, EXUTag.F),
        InstPattern(Bits.XOR,  InstType. R, EXUTag.F),
        InstPattern(Bits.SLL,  InstType. R, EXUTag.F),
        InstPattern(Bits.SRL,  InstType. R, EXUTag.T),
        InstPattern(Bits.SRA,  InstType. R, EXUTag.F),
        InstPattern(Bits.SLT,  InstType. R, EXUTag.F),
        InstPattern(Bits.SLTU, InstType. R, EXUTag.T),

        InstPattern(Bits.ADDI, InstType.IA, EXUTag.F),
        InstPattern(Bits.ANDI, InstType.IA, EXUTag.F),
        InstPattern(Bits.ORI,  InstType.IA, EXUTag.F),
        InstPattern(Bits.XORI, InstType.IA, EXUTag.F),
        InstPattern(Bits.SLLI, InstType.IA, EXUTag.F),
        InstPattern(Bits.SRLI, InstType.IA, EXUTag.T),
        InstPattern(Bits.SRAI, InstType.IA, EXUTag.F),
        InstPattern(Bits.SLTI, InstType.IA, EXUTag.F),
        InstPattern(Bits.SLTIU,InstType.IU, EXUTag.T),

        InstPattern(Bits.LB,   InstType. L, EXUTag.DontCare),
        InstPattern(Bits.LH,   InstType. L, EXUTag.DontCare),
        InstPattern(Bits.LW,   InstType. L, EXUTag.DontCare),
        InstPattern(Bits.LBU,  InstType. L, EXUTag.DontCare),
        InstPattern(Bits.LHU,  InstType. L, EXUTag.DontCare),

        InstPattern(Bits.SB,   InstType. S, EXUTag.DontCare),
        InstPattern(Bits.SH,   InstType. S, EXUTag.DontCare),
        InstPattern(Bits.SW,   InstType. S, EXUTag.DontCare),

        InstPattern(Bits.JALR, InstType.IJ, EXUTag.DontCare),
        InstPattern(Bits.JAL,  InstType. J, EXUTag.DontCare),

        InstPattern(Bits.BEQ,  InstType. B, EXUTag.DontCare),
        InstPattern(Bits.BNE,  InstType. B, EXUTag.DontCare),
        InstPattern(Bits.BGE,  InstType. B, EXUTag.DontCare),
        InstPattern(Bits.BGEU, InstType. B, EXUTag.DontCare),
        InstPattern(Bits.BLT,  InstType. B, EXUTag.DontCare),
        InstPattern(Bits.BLTU, InstType. B, EXUTag.DontCare),
        // InstPattern(BRANCH, InstType. B, EXUTag.DontCare),

        InstPattern(Bits.CSRRW, InstType.CR, EXUTag.DontCare),
        InstPattern(Bits.CSRRS, InstType.CR, EXUTag.DontCare),
        InstPattern(Bits.CSRRC, InstType.CR, EXUTag.DontCare),
        InstPattern(Bits.CSRRWI,InstType.CI, EXUTag.DontCare),
        InstPattern(Bits.CSRRSI,InstType.CI, EXUTag.DontCare),
        InstPattern(Bits.CSRRCI,InstType.CI, EXUTag.DontCare),
        // InstPattern(CSRR, InstType.CR, EXUTag.DontCare),

        InstPattern(Bits.AUIPC, InstType.UA, EXUTag.DontCare),
        InstPattern(Bits.LUI,   InstType.UL, EXUTag.DontCare),

        InstPattern(Bits.FENCE_I, InstType.FI, EXUTag.DontCare),

        InstPattern(Bits.EBREAK, InstType.EB, EXUTag.DontCare),
        InstPattern(Bits.ECALL,  InstType.EC, EXUTag.DontCare),
        InstPattern(Bits.MRET,   InstType.MR, EXUTag.DontCare)
    )

    val decodeFieldSeq = Seq(
        DecodeField.ImmTypeField,
        DecodeField.ASelField,
        DecodeField.BSelField,
        DecodeField.CSelField,
        DecodeField.DSelField,
        DecodeField.GPRRen1Field,
        DecodeField.GPRRen2Field,
        DecodeField.CSRRenField,
        DecodeField.CSRRAddrSelField,
        DecodeField.FenceIField,
        DecodeField.IsTrapField,
        DecodeField.AluAddField,
        DecodeField.EXUTagField,
        DecodeField.DNPCSelField,
        DecodeField.IsJmpField,
        DecodeField.IsBranchField,
        DecodeField.MemRenField,
        DecodeField.MemWenField,
        DecodeField.GPRWenField,
        DecodeField.GPRWSelField,
        DecodeField.CSRWenField,
        DecodeField.IsBrkField,
        DecodeField.IsIvdField
    )

    val decodeTable = new DecodeTable(instTable, decodeFieldSeq)
    
    def decode(inst: UInt, op: DecodeOPBundle): Unit = {
        val decodeResult = decodeTable.decode(inst)
        op.immType := decodeResult(DecodeField.ImmTypeField)
        op.aSel    := decodeResult(DecodeField.ASelField)
        op.bSel    := decodeResult(DecodeField.BSelField)
        op.cSel    := decodeResult(DecodeField.CSelField)
        op.dSel    := decodeResult(DecodeField.DSelField)
        op.gprRen1 := decodeResult(DecodeField.GPRRen1Field)
        op.gprRen2 := decodeResult(DecodeField.GPRRen2Field)
        op.csrRAddrSel := decodeResult(DecodeField.CSRRAddrSelField)
        op.csrRen  := decodeResult(DecodeField.CSRRenField)
        op.fenceI  := decodeResult(DecodeField.FenceIField)
        op.isTrap  := decodeResult(DecodeField.IsTrapField)
        op.aluAdd  := decodeResult(DecodeField.AluAddField)
        op.exuTag  := decodeResult(DecodeField.EXUTagField)
        op.dnpcSel := decodeResult(DecodeField.DNPCSelField)
        op.isJmp   := decodeResult(DecodeField.IsJmpField)
        op.isBranch:= decodeResult(DecodeField.IsBranchField)
        op.memRen  := decodeResult(DecodeField.MemRenField)
        op.memWen  := decodeResult(DecodeField.MemWenField)
        op.gprWen  := decodeResult(DecodeField.GPRWenField)
        op.gprWSel := decodeResult(DecodeField.GPRWSelField)
        op.csrWen  := decodeResult(DecodeField.CSRWenField)
        op.isBrk   := decodeResult(DecodeField.IsBrkField)
        op.isIvd   := decodeResult(DecodeField.IsIvdField)
    }
}
