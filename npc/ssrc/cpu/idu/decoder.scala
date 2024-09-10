package cpu.idu

import scala.collection.mutable.Map

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import cpu.exu.CmpSel
import cpu.lsu.MemType
import cpu.reg.GPRWSel
import cpu.reg.CSRWSel

object InstType extends Enumeration {
    type InstType = Value
    val R, IA, IJ, IU, L, S, B, J, UL, UA, CR, CI, EB, EC, MR, FI, IVD = Value
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

object ASel extends Enumeration {
    val ASel = Value
    val PC, GPR1, CSR, DontCare = Value
}

object BSel extends Enumeration {
    val BSel = Value
    val Imm, GPR1, GPR2, CSR = Value
}

object CSRAddrSel extends Enumeration {
    type CSRAddrSel = Value
    val N, Ins, VEC, EPC = Value
}

object Encode {
    class Tag(s: Int, l: Int) {
        val start = s
        val length = l
        val mask = (1 << l) - 1
    }

    val tags: Map[String, Tag] = Map()
    var current = 0

    def add_tag(name: String, length: Int) = {
        tags += (name -> new Tag(current, length))
        current += length
    }

    // IDU
    add_tag("ImmType",  4)
    add_tag("ASel",     2)
    add_tag("BSel",     2)
    add_tag("CSel",     1)
    add_tag("DSel",     1)
    add_tag("GPRRen1",  1)
    add_tag("GPRRen2",  1)
    add_tag("CSRRAddrSel", 2)
    add_tag("CSRRen",   1)
    add_tag("FenceI",   1)
    
    // EXU
    add_tag("AluBSel",  1) // 
    add_tag("AluAdd",   1) // for load, save, jal, jalr
    add_tag("EXUTag",   1) // for sub or unsigned

    // Jump
    add_tag("DNPCSel",  1)
    add_tag("IsJmp",    1) // no condition jump
    add_tag("IsLink",   1) // for jal and jalr
    add_tag("IsBranch", 1)

    // LSU
    add_tag("MemRen",   1)
    add_tag("MemWen",   1)
    
    // WBU
    add_tag("GPRWen",   1)
    add_tag("GPRWSel",  2)
    add_tag("CSRWAddrSel", 2)
    add_tag("CSRWen",   1)
    add_tag("IsEcall",  1)
    add_tag("IsBrk",    1)
    add_tag("IsIvd",    1)

    def gen_bitpat(attr: Map[String, Int]) : BitPat = {
        var bits: Long = 0L
        for ((name, tag) <- tags) {
            bits |= (attr(name) & tag.mask).toLong << tag.start
        }
        return BitPat(bits.U(current.W))
    }

    def get_tag(name: String, bits: UInt) : UInt = {
        val tag = tags(name)
        return bits(tag.start + tag.length - 1, tag.start)
    }

    def toInt(boolValue: Boolean): Int = if(boolValue) 1 else 0
    
    def encode (
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
            case InstType.MR => ASel. CSR // pc = mepc
            case InstType.EC => ASel.  PC // mepc = pc, pc = mtvec
            case _ => ASel.DontCare // Don't Care
        }
        m += ("ASel" -> aSel.id)

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
            case _ => BSel.Imm // Dont care
        }
        m += ("BSel" -> bSel.id)

        val cSel = Seq(
            InstType.IJ,
            InstType. J
        ).contains(instType)
        m += ("CSel" -> toInt(cSel))

        val dSel = instType == InstType.L
        m += ("DSel" -> toInt(dSel))
            
        val gprRen1 = aSel == ASel.GPR1 || bSel == BSel.GPR1 || instType == InstType.B
        m += ("GPRRen1" -> toInt(gprRen1)) 
        val gprRen2 = bSel == BSel.GPR2 || Seq(InstType.S, InstType.B).contains(instType)
        m += ("GPRRen2" -> toInt(gprRen2))
        val csrRen = aSel == ASel.CSR || bSel == BSel.CSR
        m += ("CSRRen" -> toInt(csrRen))

        val csrRRAddrSel = instType match {
            case InstType.EC => CSRAddrSel.VEC
            case InstType.MR => CSRAddrSel.EPC
            case InstType.CR => CSRAddrSel.Ins
            case InstType.CI => CSRAddrSel.Ins
            case _ => CSRAddrSel.N
        }
        m += ("CSRRAddrSel"-> csrRRAddrSel.id)

        val fenceI = instType == InstType.FI
        m += ("FenceI" -> toInt(fenceI))

        val aluBSel = Seq(
            InstType.UL, // lui
            InstType.MR  // mret
        ).contains(instType)
        m += ("AluBSel" -> toInt(aluBSel))

        val aluAdd = Seq(
            InstType. L, // load
            InstType. S, // save
            InstType.UA, // auipc
            InstType. B, // branch
            InstType. J  // jal
        ).contains(instType)
        m += ("AluAdd" -> toInt(aluAdd))

        m += ("EXUTag" -> toInt(exuTag == EXUTag.T))

        val dnpcSel = Seq(
            InstType.EC,
            InstType.MR
        ).contains(instType)
        m += ("DNPCSel" -> toInt(dnpcSel))

        val isJmp = Seq(
            InstType. J,
            InstType.IJ,
            InstType.MR,
            InstType.EC
        ).contains(instType)
        m += ("IsJmp" -> toInt(isJmp))

        val isLink = Seq(
            InstType. J,
            InstType.IJ
        ).contains(instType)
        m += ("IsLink" -> toInt(isLink))

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

        val csrWAddrSel = instType match {
            case InstType.EC => CSRAddrSel.EPC
            case InstType.CR => CSRAddrSel.Ins
            case InstType.CI => CSRAddrSel.Ins
            case _ => CSRAddrSel.N
        }
        m += ("CSRWAddrSel" -> csrWAddrSel.id)

        val csrWen = csrWAddrSel == CSRAddrSel.N
        m += ("CSRWen" -> toInt(csrWen))

        val isEcall = instType == InstType.EC
        m += ("IsEcall" -> toInt(isEcall))

        val isBrk = instType == InstType.EB
        m += ("IsBrk" -> toInt(isBrk))

        val isIvd = instType == InstType.IVD
        m += ("IsIvd" -> toInt(isIvd))

        return gen_bitpat(m)
    }
    
    def encode_r (exuTag: EXUTag) : BitPat = encode(InstType. R, exuTag)
    def encode_ia(exuTag: EXUTag) : BitPat = encode(InstType.IA, exuTag)
    def encode_iu() : BitPat = encode(InstType.IU, EXUTag.T)
    def encode_load() : BitPat = encode(InstType. L, EXUTag.DontCare)
    def encode_save() : BitPat = encode(InstType. S, EXUTag.DontCare)
    def encode_jump(instType: InstType) : BitPat = encode(instType, EXUTag.DontCare)
    def encode_brch() : BitPat = encode(InstType. B, EXUTag.DontCare)
    def encode_csrr() : BitPat = encode(InstType.CR, EXUTag.DontCare)
    def encode_csri() : BitPat = encode(InstType.CI, EXUTag.DontCare)
}

class OP(bits : UInt) {
    // IDU
    val immType = Encode.get_tag("ImmType", bits)
    val aSel = Encode.get_tag("ASel", bits)
    val bSel = Encode.get_tag("BSel", bits)
    val cSel = Encode.get_tag("CSel", bits).asBool
    val dSel = Encode.get_tag("DSel", bits).asBool
    val gprRen1 = Encode.get_tag("GPRRen1", bits).asBool
    val gprRen2 = Encode.get_tag("GPRRen2", bits).asBool
    val csrRAddrSel = Encode.get_tag("CSRRAddrSel", bits)
    val csrWAddrSel = Encode.get_tag("CSRWAddrSel", bits)
    val csrRen = Encode.get_tag("CSRRen", bits).asBool
    val fenceI = Encode.get_tag("FenceI", bits).asBool

    // EXU
    val aluBSel = Encode.get_tag("AluBSel", bits).asBool
    val aluAdd = Encode.get_tag("AluAdd", bits).asBool
    val exuTag = Encode.get_tag("EXUTag", bits).asBool

    // Jump
    val dnpcSel = Encode.get_tag("DNPCSel", bits).asBool
    val isJmp = Encode.get_tag("IsJmp", bits).asBool
    val isBranch = Encode.get_tag("IsBranch", bits).asBool
    
    // LSU
    val memRen = Encode.get_tag("MemRen", bits).asBool
    val memWen = Encode.get_tag("MemWen", bits).asBool

    // WBU
    val gprWen = Encode.get_tag("GPRWen", bits).asBool
    val gprWSel = Encode.get_tag("GPRWSel", bits)
    val csrWen = Encode.get_tag("CSRWen", bits).asBool
    val isEcall = Encode.get_tag("IsEcall", bits).asBool
    val isBrk = Encode.get_tag("IsBrk", bits).asBool
    val isIvd = Encode.get_tag("IsIvd", bits).asBool
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

    val SB      = BitPat("b???????_?????_?????_000_?????_010_0011")
    val SH      = BitPat("b???????_?????_?????_001_?????_010_0011")
    val SW      = BitPat("b???????_?????_?????_010_?????_010_0011")

    val JAL     = BitPat("b????????????????????_?????_110_1111")

    val BEQ     = BitPat("b???????_?????_?????_000_?????_110_0011")
    val BGE     = BitPat("b???????_?????_?????_101_?????_110_0011")
    val BGEU    = BitPat("b???????_?????_?????_111_?????_110_0011")
    val BLT     = BitPat("b???????_?????_?????_100_?????_110_0011")
    val BLTU    = BitPat("b???????_?????_?????_110_?????_110_0011")
    val BNE     = BitPat("b???????_?????_?????_001_?????_110_0011")

    val AUIPC   = BitPat("b?????????????????????_?????_001_0111")
    val LUI     = BitPat("b?????????????????????_?????_011_0111")

    val FENCE_I = BitPat("b??????????????????_001_?????_000_1111")

    val CSRRW   = BitPat("b????????????_?????_001_?????_111_0011")
    val CSRRS   = BitPat("b????????????_?????_010_?????_111_0011")
    val CSRRC   = BitPat("b????????????_?????_011_?????_111_0011")
    val CSRRWI  = BitPat("b????????????_?????_101_?????_111_0011")
    val CSRRSI  = BitPat("b????????????_?????_110_?????_111_0011")
    val CSRRCI  = BitPat("b????????????_?????_111_?????_111_0011")
    
    val EBREAK  = BitPat("b0000000000010000000000000_1110011")
    val ECALL   = BitPat("b0000000000000000000000000_1110011")
    val MRET    = BitPat("b0011000000100000000000000_1110011")

    val truthTable = TruthTable(
        Map(      
            ADD     -> Encode.encode_r(EXUTag.F),
            SUB     -> Encode.encode_r(EXUTag.T),
            AND     -> Encode.encode_r(EXUTag.F),
            OR      -> Encode.encode_r(EXUTag.F),
            XOR     -> Encode.encode_r(EXUTag.F),
            SLL     -> Encode.encode_r(EXUTag.F),
            SRL     -> Encode.encode_r(EXUTag.F),
            SRA     -> Encode.encode_r(EXUTag.F),
            SLT     -> Encode.encode_r(EXUTag.F),
            SLTU    -> Encode.encode_r(EXUTag.T),

            ADDI    -> Encode.encode_ia(EXUTag.F),
            ANDI    -> Encode.encode_ia(EXUTag.F),
            ORI     -> Encode.encode_ia(EXUTag.F),
            XORI    -> Encode.encode_ia(EXUTag.F),
            SLLI    -> Encode.encode_ia(EXUTag.F),
            SRLI    -> Encode.encode_ia(EXUTag.T),
            SRAI    -> Encode.encode_ia(EXUTag.F),
            SLTI    -> Encode.encode_ia(EXUTag.F),
            SLTIU   -> Encode.encode_iu(),

            LB      -> Encode.encode_load(),
            LH      -> Encode.encode_load(),
            LW      -> Encode.encode_load(),
            LBU     -> Encode.encode_load(),
            LHU     -> Encode.encode_load(),

            SB      -> Encode.encode_save(),
            SH      -> Encode.encode_save(),
            SW      -> Encode.encode_save(),

            JALR    -> Encode.encode_jump(InstType.IJ),
            JAL     -> Encode.encode_jump(InstType. J),

            BEQ     -> Encode.encode_brch(),
            BNE     -> Encode.encode_brch(),
            BGE     -> Encode.encode_brch(),
            BGEU    -> Encode.encode_brch(),
            BLT     -> Encode.encode_brch(),
            BLTU    -> Encode.encode_brch(),

            CSRRW   -> Encode.encode_csrr(),
            CSRRS   -> Encode.encode_csrr(),
            CSRRC   -> Encode.encode_csrr(),
            CSRRWI  -> Encode.encode_csri(),
            CSRRSI  -> Encode.encode_csri(),
            CSRRCI  -> Encode.encode_csri(),

            AUIPC   -> Encode.encode(InstType.UA, EXUTag.DontCare),
            LUI     -> Encode.encode(InstType.UL, EXUTag.DontCare),
            
            FENCE_I -> Encode.encode(InstType.FI, EXUTag.DontCare),
            
            EBREAK  -> Encode.encode(InstType.EB, EXUTag.DontCare),
            ECALL   -> Encode.encode(InstType.EC, EXUTag.DontCare),
            MRET    -> Encode.encode(InstType.MR, EXUTag.DontCare)
        ),
        default = Encode.encode(InstType.IVD, EXUTag.DontCare)
    )

    def decode(inst: UInt) : OP = {
        return new OP(decoder(inst, truthTable))
    }
}