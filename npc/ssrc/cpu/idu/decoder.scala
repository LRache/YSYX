package cpu.idu

import scala.collection.mutable.Map

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import cpu.exu.CmpSel
import cpu.lsu.MemType
import cpu.reg.GPRWSel
import scala.collection.mutable.ArrayBuffer
import cpu.Config
import cpu.idu.CExtensionEncode.encode

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

object ImmType extends Enumeration {
    type ImmType = Value
    val N, I, IU, S, B, U, J, C = Value
}

object EXUTag extends Enumeration {
    type EXUTag = Value
    val F, T, DontCare = Value
}
import EXUTag.EXUTag

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

object CSRAddrSel extends Enumeration {
    type CSRAddrSel = Value
    val N, Ins, VEC, EPC = Value
}

object NormalEncode {
    val encoder = new BitPatEncoder()

    // IDU
    encoder.add_tag("ImmType",  4)
    encoder.add_tag("ASel",     2)
    encoder.add_tag("BSel",     2)
    encoder.add_tag("CSel",     1)
    encoder.add_tag("DSel",     1)
    encoder.add_tag("GPRRen1",  1)
    encoder.add_tag("GPRRen2",  1)
    encoder.add_tag("CSRRAddrSel", 2)
    encoder.add_tag("CSRRen",   1)
    encoder.add_tag("FenceI",   1)
    encoder.add_tag("IsTrap",  1)
    
    // EXU
    encoder.add_tag("AluAdd",   1) // for load, save, jal, jalr
    encoder.add_tag("EXUTag",   1) // for sub or unsigned

    // Jump
    encoder.add_tag("DNPCSel",  1)
    encoder.add_tag("IsJmp",    1) // no condition jump
    encoder.add_tag("IsBranch", 1)

    // LSU
    encoder.add_tag("MemRen",   1)
    encoder.add_tag("MemWen",   1)
    
    // WBU
    encoder.add_tag("GPRWen",   1)
    encoder.add_tag("GPRWSel",  2)
    encoder.add_tag("CSRWen",   1)
    encoder.add_tag("IsBrk",    1)
    encoder.add_tag("IsIvd",    1)

    def get_tag(name: String, bits: UInt) : UInt = encoder.get_tag(name, bits)

    def toInt(boolValue: Boolean): Int = if(boolValue) 1 else 0
    
    def apply (
        instType: InstType,
        exuTag:   EXUTag.EXUTag
    ): BitPat = {
        val m: Map[String, Int] = Map()
            
        val immType = instType match {
            case InstType.IA => ImmType. I.id
            case InstType.IJ => ImmType. I.id
            case InstType. L => ImmType. I.id
            case InstType.IU => ImmType.IU.id
            case InstType. S => ImmType. S.id
            case InstType. B => ImmType. B.id
            case InstType.UA => ImmType. U.id
            case InstType.UL => ImmType. U.id
            case InstType. J => ImmType. J.id
            case InstType.CI => ImmType. C.id
            case _           => ImmType. N.id
        }
        m += ("ImmType" -> immType)

        val aSel = instType match {
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
            case InstType.IVD => ASel. PC
            case _ => ASel.ZERO // Don't Care
        }
        m += ("ASel" -> aSel)

        val bSel = instType match {
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
            case InstType.IVD => BSel.CSR
            case InstType.MR => BSel. CSR
            case _ => BSel.DontCare // Dont care
        }
        m += ("BSel" -> bSel)

        val cSel = Seq(
            InstType.IJ,
            InstType. J
        ).contains(instType)
        m += ("CSel" -> toInt(cSel))

        val dSel = Seq(InstType.IA, InstType.IU).contains(instType)
        m += ("DSel" -> toInt(dSel))
            
        val gprRen1 = aSel == ASel.GPR1 || bSel == BSel.GPR1 || instType == InstType.B
        m += ("GPRRen1" -> toInt(gprRen1)) 
        val gprRen2 = bSel == BSel.GPR2 || Seq(InstType.S, InstType.B).contains(instType)
        m += ("GPRRen2" -> toInt(gprRen2))
        val csrRen = aSel == ASel.CSR || bSel == BSel.CSR
        m += ("CSRRen" -> toInt(csrRen))

        val isTrap = Seq(InstType.EC, InstType.IVD).contains(instType)
        m += ("IsTrap" -> toInt(isTrap))

        val csrRRAddrSel = if(isTrap) CSRAddrSel.VEC else instType match {
            case InstType.MR => CSRAddrSel.EPC
            case InstType.CR => CSRAddrSel.Ins
            case InstType.CI => CSRAddrSel.Ins
            case _ => CSRAddrSel.N
        }
        m += ("CSRRAddrSel"-> csrRRAddrSel.id)

        val fenceI = instType == InstType.FI
        m += ("FenceI" -> toInt(fenceI))

        val aluAdd = Seq(
            InstType. L, // load
            InstType. S, // save
            InstType.UA, // auipc
            InstType. B, // branch
            InstType. J, // jal
            InstType.UL, // lui
            InstType.MR  // mret
        ).contains(instType)
        m += ("AluAdd" -> toInt(aluAdd))

        m += ("EXUTag" -> toInt(exuTag == EXUTag.T))

        val dnpcSel = instType == InstType.MR || isTrap
        m += ("DNPCSel" -> toInt(dnpcSel))

        val isJmp = Seq(
            InstType. J,
            InstType.IJ,
            InstType.MR
        ).contains(instType)
        m += ("IsJmp" -> toInt(isJmp))

        val isBranch = instType == InstType.B
        m += ("IsBranch" -> toInt(isBranch))

        val memRen = instType == InstType.L
        m += ("MemRen" -> toInt(memRen))

        val memWen = instType == InstType.S
        m += ("MemWen" -> toInt(memWen))

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
        ).contains(instType)
        m += ("GPRWen" -> toInt(gprWen))

        val gprWSel = instType match {
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
        m += ("GPRWSel" -> gprWSel)

        val csrWen = Seq(InstType.CR, InstType.CI).contains(instType)
        m += ("CSRWen" -> toInt(csrWen))

        val isBrk = instType == InstType.EB
        m += ("IsBrk" -> toInt(isBrk))

        val isIvd = instType == InstType.IVD
        m += ("IsIvd" -> toInt(isIvd))

        return encoder.gen_bitpat(m)
    }
    
    def encode_r (exuTag: EXUTag) : BitPat = apply(InstType. R, exuTag)
    def encode_ia(exuTag: EXUTag) : BitPat = apply(InstType.IA, exuTag)
    def encode_iu() : BitPat = apply(InstType.IU, EXUTag.T)
    def encode_load() : BitPat = apply(InstType. L, EXUTag.DontCare)
    def encode_save() : BitPat = apply(InstType. S, EXUTag.DontCare)
    def encode_jump(instType: InstType) : BitPat = apply(instType, EXUTag.DontCare)
    def encode_brch() : BitPat = apply(InstType. B, EXUTag.DontCare)
    def encode_csrr() : BitPat = apply(InstType.CR, EXUTag.DontCare)
    def encode_csri() : BitPat = apply(InstType.CI, EXUTag.DontCare)
}

class OP(bits : UInt) {
    // IDU
    val immType = NormalEncode.get_tag("ImmType", bits)
    val aSel = NormalEncode.get_tag("ASel", bits)
    val bSel = NormalEncode.get_tag("BSel", bits)
    val cSel = NormalEncode.get_tag("CSel", bits).asBool
    val dSel = NormalEncode.get_tag("DSel", bits).asBool
    val gprRen1 = NormalEncode.get_tag("GPRRen1", bits).asBool
    val gprRen2 = NormalEncode.get_tag("GPRRen2", bits).asBool
    val csrRAddrSel = NormalEncode.get_tag("CSRRAddrSel", bits)
    val csrRen = NormalEncode.get_tag("CSRRen", bits).asBool
    val fenceI = NormalEncode.get_tag("FenceI", bits).asBool
    val isTrap = NormalEncode.get_tag("IsTrap", bits).asBool

    // EXU
    val aluAdd = NormalEncode.get_tag("AluAdd", bits).asBool
    val exuTag = NormalEncode.get_tag("EXUTag", bits).asBool

    // Jump
    val dnpcSel = NormalEncode.get_tag("DNPCSel", bits).asBool
    val isJmp = NormalEncode.get_tag("IsJmp", bits).asBool
    val isBranch = NormalEncode.get_tag("IsBranch", bits).asBool
    
    // LSU
    val memRen = NormalEncode.get_tag("MemRen", bits).asBool
    val memWen = NormalEncode.get_tag("MemWen", bits).asBool

    // WBU
    val gprWen = NormalEncode.get_tag("GPRWen", bits).asBool
    val gprWSel = NormalEncode.get_tag("GPRWSel", bits)
    val csrWen = NormalEncode.get_tag("CSRWen", bits).asBool
    val isBrk = NormalEncode.get_tag("IsBrk", bits).asBool
    val isIvd = NormalEncode.get_tag("IsIvd", bits).asBool
}

object Decoder {
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

    val AUIPC   = BitPat("b?????????????????????_?????_001_0111")
    val LUI     = BitPat("b?????????????????????_?????_011_0111")

    val FENCE_I = BitPat("b??????????????????_001_?????_000_1111")

    val CSRRW   = BitPat("b????????????_?????_001_?????_111_0011")
    val CSRRS   = BitPat("b????????????_?????_010_?????_111_0011")
    val CSRRC   = BitPat("b????????????_?????_011_?????_111_0011")
    val CSRRWI  = BitPat("b????????????_?????_101_?????_111_0011")
    val CSRRSI  = BitPat("b????????????_?????_110_?????_111_0011")
    val CSRRCI  = BitPat("b????????????_?????_111_?????_111_0011")
    
    val EBREAK  = BitPat("b00000000000100000000_00000_111_0011")
    val ECALL   = BitPat("b00000000000000000000_00000_111_0011")
    val MRET    = BitPat("b00110000001000000000_00000_111_0011")

    val truthTable = TruthTable(
        Map(      
            ADD     -> NormalEncode.encode_r(EXUTag.F),
            SUB     -> NormalEncode.encode_r(EXUTag.T),
            AND     -> NormalEncode.encode_r(EXUTag.F),
            OR      -> NormalEncode.encode_r(EXUTag.F),
            XOR     -> NormalEncode.encode_r(EXUTag.F),
            SLL     -> NormalEncode.encode_r(EXUTag.F),
            SRL     -> NormalEncode.encode_r(EXUTag.T),
            SRA     -> NormalEncode.encode_r(EXUTag.F),
            SLT     -> NormalEncode.encode_r(EXUTag.F),
            SLTU    -> NormalEncode.encode_r(EXUTag.T),

            ADDI    -> NormalEncode.encode_ia(EXUTag.F),
            ANDI    -> NormalEncode.encode_ia(EXUTag.F),
            ORI     -> NormalEncode.encode_ia(EXUTag.F),
            XORI    -> NormalEncode.encode_ia(EXUTag.F),
            SLLI    -> NormalEncode.encode_ia(EXUTag.F),
            SRLI    -> NormalEncode.encode_ia(EXUTag.T),
            SRAI    -> NormalEncode.encode_ia(EXUTag.F),
            SLTI    -> NormalEncode.encode_ia(EXUTag.F),
            SLTIU   -> NormalEncode.encode_iu(),

            LB      -> NormalEncode.encode_load(),
            LH      -> NormalEncode.encode_load(),
            LW      -> NormalEncode.encode_load(),
            LBU     -> NormalEncode.encode_load(),
            LHU     -> NormalEncode.encode_load(),
            // LOAD    -> NormalEncode.encode_load(),

            SB      -> NormalEncode.encode_save(),
            SH      -> NormalEncode.encode_save(),
            SW      -> NormalEncode.encode_save(),
            // SAVE    -> NormalEncode.encode_save(),

            JALR    -> NormalEncode.encode_jump(InstType.IJ),
            JAL     -> NormalEncode.encode_jump(InstType. J),

            BEQ     -> NormalEncode.encode_brch(),
            BNE     -> NormalEncode.encode_brch(),
            BGE     -> NormalEncode.encode_brch(),
            BGEU    -> NormalEncode.encode_brch(),
            BLT     -> NormalEncode.encode_brch(),
            BLTU    -> NormalEncode.encode_brch(),
            // BRANCH  -> NormalEncode.encode_brch(),

            CSRRW   -> NormalEncode.encode_csrr(),
            CSRRS   -> NormalEncode.encode_csrr(),
            CSRRC   -> NormalEncode.encode_csrr(),
            CSRRWI  -> NormalEncode.encode_csri(),
            CSRRSI  -> NormalEncode.encode_csri(),
            CSRRCI  -> NormalEncode.encode_csri(),
            // CSRR     -> NormalEncode.encode_csrr(),

            AUIPC   -> NormalEncode(InstType.UA, EXUTag.DontCare),
            LUI     -> NormalEncode(InstType.UL, EXUTag.DontCare),
            
            FENCE_I -> NormalEncode(InstType.FI, EXUTag.DontCare),
            
            EBREAK  -> NormalEncode(InstType.EB, EXUTag.DontCare),
            ECALL   -> NormalEncode(InstType.EC, EXUTag.DontCare),
            MRET    -> NormalEncode(InstType.MR, EXUTag.DontCare)
        ),
        default = NormalEncode(InstType.IVD, EXUTag.DontCare),
    )

    def decode(inst: UInt) : OP = {
        return new OP(decoder(inst, truthTable))
    }
}