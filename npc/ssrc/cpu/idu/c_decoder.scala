package cpu.idu

import scala.collection.mutable.Map

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

object CInstType extends Enumeration {
    type CInstType = Value
    val SL, SS, RL, RS, J, JAL, JR, JALR, B, LI, LUI, RAU, ADD16, ADD4, RA, IVD = Value
}

object CImmType extends Enumeration {
    type CImmType = Value
    val SL, SS, RLS, JI, B, LI, LUI, I16, I4, IA = Value
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
    add_tag("DSel",     1)
    add_tag("GPRRen1",  1)
    add_tag("GPRRen2",  1)
    add_tag("GPRRaddr1",2)
    add_tag("GPRRaddr2",1)
    add_tag("GPRWaddr", 2)
    
    // EXU
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

    def encode(instType: CInstType, exuTag: EXUTag) : BitPat = {
        val m: Map[String, Int] = Map()
        
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

    val truthTable = TruthTable(
        Map(
            LWSP    -> encode(CInstType. SL, EXUTag.DontCare),
            SWSP    -> encode(CInstType. SS, EXUTag.DontCare),
            LW      -> encode(CInstType. RL, EXUTag.DontCare),
            SW      -> encode(CInstType. RS, EXUTag.DontCare),

            J       -> encode(CInstType.J   , EXUTag.DontCare),
            JR      -> encode(CInstType.JR  , EXUTag.DontCare),
            JAL     -> encode(CInstType.JAL , EXUTag.DontCare),
            JALR    -> encode(CInstType.JALR, EXUTag.DontCare),
            
            BEQZ    -> encode(CInstType.B, EXUTag.DontCare),
            BNEZ    -> encode(CInstType.B, EXUTag.DontCare),

            LI      -> encode(CInstType.LI , EXUTag.DontCare),
            LUI     -> encode(CInstType.LUI, EXUTag.DontCare),

            ADDI    -> encode(CInstType.RAU, EXUTag.T),
            ADDI16SP -> encode(CInstType.ADD16, EXUTag.DontCare),
            ADDI4SPN -> encode(CInstType.ADD4, EXUTag.DontCare),
            
            SLLI    -> encode(CInstType.RA, EXUTag.DontCare),
            // SRLI    -> encode(CInstType.)
        ),
        encode(CInstType.IVD, EXUTag.DontCare)
    )
}