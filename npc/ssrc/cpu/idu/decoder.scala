package cpu.idu

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import cpu.exu.AluSel
import cpu.exu.CmpSel
import cpu.lsu.MemType
import cpu.reg.GPRWSel
import cpu.reg.CSRWSel

import AluSel.AluSel
import CmpSel.CmpSel
import GPRWSel.GPRWSel
import CSRWSel.CSRWSel
import cpu.idu.CSRAddrSel.Ins

object InstType extends Enumeration {
    type InstType = Value
    val R, IA, IJ, IU, IL, S, B, J, UL, UA, N, C, EB, EC, MR, IVD = Value
    // UL for LUI
    // UA for AUIPC
}
import InstType.InstType

object ImmType extends Enumeration {
    type ImmType = Value
    val N, I, IU, S, B, U, J = Value
}

object CSRAddrSel extends Enumeration {
    type CSRAddrSel = Value
    val N, Ins, VEC, EPC = Value
}

object Encode {
    object Pos {
        val BoolLen = 1

        val ImmType = 0
        val ImmTypeL= 4
        
        val ALUSel  = ImmType + ImmTypeL
        val ALUSelL = 4
        
        val ASel    = ALUSel + ALUSelL
        
        val BSel    = ASel + BoolLen
        
        val CmpSel  = BSel + BoolLen
        val CmpSelL = 3

        val GPRWSel = CmpSel + CmpSelL
        val GPRWSelL= 2

        val GPRWen  = GPRWSel + GPRWSelL

        val MemWen  = GPRWen + BoolLen
        val MemRen  = MemWen + BoolLen
        val IsJmp   = MemRen + BoolLen
        val IsBrk   = IsJmp + BoolLen
        val IsIvd   = IsBrk + BoolLen
        val CSRWen  = IsIvd + BoolLen

        val CSRWSel = CSRWen + BoolLen
        val CSRWSelL= 3

        // val Rs1Sel = CSRWSel + CSRWSelL
        // val Rs2Sel = Rs1Sel + BoolLen

        // val DNPCSel = Rs2Sel + BoolLen
        val DNPCSel = CSRWSel + CSRWSelL

        val CSRWAddrSel = DNPCSel + BoolLen
        val CSRWAddrSelL = 2

        val CSRRAddrSel = CSRWAddrSel + CSRWAddrSelL
        val CSRRAddrSelL = 2

        val IsECall = CSRRAddrSel + CSRRAddrSelL
    }
    def toInt(boolValue: Boolean): Int = if(boolValue) 1 else 0
    
    def encode (
        instType: InstType, 
        _alu_sel:   AluSel,
        _cmp_sel:   CmpSel,
        _csr_sel:   CSRWSel,
        ): BitPat = {
            var alu_sel = _alu_sel.id
            val cmp_sel = _cmp_sel.id
            
            val is_brk  = toInt(instType == InstType.EB)
            val is_jmp  = toInt(
                (instType == InstType.IJ) || 
                (instType == InstType. J) || 
                (instType == InstType.EC) ||
                (instType == InstType. B) ||
                (instType == InstType.MR)
            )
            val mem_wen = toInt((instType == InstType. S))
            val mem_ren = toInt((instType == InstType.IL))
            val reg_ws = instType match {
                case InstType. R => GPRWSel.EXU.id
                case InstType.IA => GPRWSel.EXU.id
                case InstType.IU => GPRWSel.EXU.id
                case InstType.IL => GPRWSel.MEM.id
                case InstType.IJ => GPRWSel. SN.id
                case InstType. J => GPRWSel. SN.id
                case InstType.UL => GPRWSel.EXU.id
                case InstType.UA => GPRWSel.EXU.id
                // case InstType.CR => GPRWSel.CSR.id
                case InstType.C  => GPRWSel.CSR.id
                case _           => GPRWSel.EXU.id
            }
            val REG_WEN_SEQ = Seq(
                InstType. R,
                InstType.IA,
                InstType.IU,
                InstType.IL,
                InstType.IJ,
                InstType. J,
                InstType.UL,
                InstType.UA,
                InstType. C
            )
            val reg_wen = toInt(REG_WEN_SEQ.contains(instType))
            val is_invalid = toInt(instType == InstType.IVD)
            val a_sel = toInt(
                (instType == InstType. J) ||
                (instType == InstType. B) ||
                (instType == InstType.UA)
            )
            val b_sel = toInt(
                (instType == InstType.IA) ||
                (instType == InstType.IU) ||
                (instType == InstType.IL) ||
                (instType == InstType.IJ) ||
                (instType == InstType. S) ||
                (instType == InstType. J) ||
                (instType == InstType. B) ||
                (instType == InstType.UA) ||
                (instType == InstType.UL)
            )
            val imm_type = instType match {
                case InstType.IA => ImmType. I.id
                case InstType.IJ => ImmType. I.id
                case InstType.IL => ImmType. I.id
                case InstType.IU => ImmType.IU.id
                case InstType. S => ImmType. S.id
                case InstType. B => ImmType. B.id
                case InstType.UA => ImmType. U.id
                case InstType.UL => ImmType. U.id
                case InstType. J => ImmType. J.id
                case _           => ImmType. N.id
            }
            // val csr_wen = toInt(instType == InstType.CR || instType == InstType.CI)
            val csr_wen = toInt(instType == InstType.C)
            val csr_wsel = _csr_sel.id
            val dnpc_sel = toInt(instType == InstType.EC || instType == InstType.MR)
            val csr_waddr_sel = instType match {
                case InstType.EC => CSRAddrSel.EPC.id
                // case InstType.CR => CSRAddrSel.Ins.id
                // case InstType.CI => CSRAddrSel.Ins.id
                case InstType. C => CSRAddrSel.Ins.id
                case _           => CSRAddrSel.  N.id
            }
            val csr_raddr_sel = instType match {
                case InstType.EC => CSRAddrSel.VEC.id
                case InstType.MR => CSRAddrSel.EPC.id
                // case InstType.CR => CSRAddrSel.Ins.id
                // case InstType.CI => CSRAddrSel.Ins.id
                case InstType. C => CSRAddrSel.Ins.id
                case _           => CSRAddrSel.  N.id
            }
            val is_ecall = toInt(instType == InstType.EC)
            // val rs1Sel = toInt(instType == InstType.CI)
            // val rs2Sel = toInt(instType == InstType.CR || instType == InstType.CI)
            
            var bits: Long = 0L
            bits |= (imm_type   & 0b1111).toLong << Pos.ImmType
            bits |= (alu_sel    & 0b1111).toLong << Pos.ALUSel
            bits |= (a_sel      & 0b1   ).toLong << Pos.ASel
            bits |= (b_sel      & 0b1   ).toLong << Pos.BSel
            bits |= (cmp_sel    & 0b111 ).toLong << Pos.CmpSel
            bits |= (reg_ws     & 0b111 ).toLong << Pos.GPRWSel
            bits |= (reg_wen    & 0b1   ).toLong << Pos.GPRWen
            bits |= (mem_wen    & 0b1   ).toLong << Pos.MemWen
            bits |= (mem_ren    & 0b1   ).toLong << Pos.MemRen
            bits |= (is_jmp     & 0b1   ).toLong << Pos.IsJmp
            bits |= (is_brk     & 0b1   ).toLong << Pos.IsBrk
            bits |= (is_invalid & 0b1   ).toLong << Pos.IsIvd
            bits |= (csr_wen    & 0b1   ).toLong << Pos.CSRWen
            bits |= (csr_wsel   & 0b111 ).toLong << Pos.CSRWSel
            // bits |= (rs1Sel     & 0b1   ).toLong << Pos.Rs1Sel
            // bits |= (rs2Sel     & 0b1   ).toLong << Pos.Rs2Sel
            bits |= (dnpc_sel   & 0b1   ).toLong << Pos.DNPCSel
            bits |= (csr_waddr_sel & 0b11).toLong << Pos.CSRWAddrSel
            bits |= (csr_raddr_sel & 0b11).toLong << Pos.CSRRAddrSel
            bits |= (is_ecall   & 0b1   ).toLong << Pos.IsECall
            return BitPat(bits.U(48.W))
        }
    
    def encode_r(alu_sel: AluSel) : BitPat = encode(InstType.R, alu_sel, CmpSel.N, CSRWSel.W)
    def encode_i(instType: InstType, alu_sel: AluSel) : BitPat = encode(instType, alu_sel, CmpSel.  N, CSRWSel.W)
    def encode_ia(alu_sel: AluSel) : BitPat = encode_i(InstType.IA, alu_sel)
    def encode_iu(alu_sel: AluSel) : BitPat = encode_i(InstType.IU, alu_sel)
    def encode_load() : BitPat = encode(InstType.IL, AluSel.ADD, CmpSel.N, CSRWSel.W)
    def encode_save() : BitPat = encode(InstType. S, AluSel.ADD, CmpSel.N, CSRWSel.W)
    def encode_jump(instType: InstType) : BitPat = encode(instType, AluSel.ADD, CmpSel.Y, CSRWSel.W)
    def encode_brch(cmpSel: CmpSel) : BitPat = encode(InstType.B, AluSel.ADD, cmpSel, CSRWSel.W)
    def encode_csr(csrWSel: CSRWSel) : BitPat = encode(InstType.C, AluSel.N, CmpSel.N, csrWSel)
}

import Encode.Pos
class OP(t : UInt) {
    val immType = t(Pos.ImmType + Pos.ImmTypeL  - 1, Pos.ImmType)
    val aluSel  = t(Pos.ALUSel  + Pos.ALUSelL   - 1, Pos.ALUSel)
    val aSel    = t(Pos.ASel    + Pos.BoolLen   - 1, Pos.ASel).asBool
    val bSel    = t(Pos.BSel    + Pos.BoolLen   - 1, Pos.BSel).asBool
    val cmpSel  = t(Pos.CmpSel  + Pos.CSRWSelL  - 1, Pos.CmpSel)
    val gprWSel = t(Pos.GPRWSel + Pos.GPRWSelL  - 1, Pos.GPRWSel)
    val gprWen  = t(Pos.GPRWen  + Pos.BoolLen   - 1, Pos.GPRWen).asBool
    val memWen  = t(Pos.MemWen  + Pos.BoolLen   - 1, Pos.MemWen).asBool
    val menRen  = t(Pos.MemRen  + Pos.BoolLen   - 1, Pos.MemRen).asBool
    val isJmp   = t(Pos.IsJmp   + Pos.BoolLen   - 1, Pos.IsJmp).asBool
    val isBrk   = t(Pos.IsBrk   + Pos.BoolLen   - 1, Pos.IsBrk).asBool
    val isIvd   = t(Pos.IsIvd   + Pos.BoolLen   - 1, Pos.IsIvd).asBool
    // val rs1Sel  = t(Pos.Rs1Sel  + Pos.BoolLen   - 1, Pos.Rs1Sel).asBool
    // val rs2Sel  = t(Pos.Rs2Sel  + Pos.BoolLen   - 1, Pos.Rs2Sel).asBool
    val csrWen  = t(Pos.CSRWen  + Pos.BoolLen   - 1, Pos.CSRWen).asBool
    val dnpcSel = t(Pos.DNPCSel + Pos.BoolLen   - 1, Pos.DNPCSel).asBool
    val csrWASel= t(Pos.CSRWAddrSel + Pos.CSRWAddrSelL - 1, Pos.CSRWAddrSel)
    val csrRASel= t(Pos.CSRRAddrSel + Pos.CSRRAddrSelL - 1, Pos.CSRRAddrSel)
    val isEcall = t(Pos.IsECall + Pos.BoolLen   - 1, Pos.IsECall).asBool
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
            ADD     -> Encode.encode_r(AluSel. ADD),
            SUB     -> Encode.encode_r(AluSel. SUB),
            AND     -> Encode.encode_r(AluSel. AND),
            OR      -> Encode.encode_r(AluSel.  OR),
            XOR     -> Encode.encode_r(AluSel. XOR),
            SLL     -> Encode.encode_r(AluSel. SLL),
            SRL     -> Encode.encode_r(AluSel. SRL),
            SRA     -> Encode.encode_r(AluSel. SRA),
            SLT     -> Encode.encode_r(AluSel. SLT),
            SLTU    -> Encode.encode_r(AluSel.SLTU),

            ADDI    -> Encode.encode_ia(AluSel. ADD),
            ANDI    -> Encode.encode_ia(AluSel. AND),
            ORI     -> Encode.encode_ia(AluSel.  OR),
            XORI    -> Encode.encode_ia(AluSel. XOR),
            SLLI    -> Encode.encode_ia(AluSel. SLL),
            SRLI    -> Encode.encode_ia(AluSel. SRL),
            SRAI    -> Encode.encode_ia(AluSel. SRA),
            SLTI    -> Encode.encode_ia(AluSel. SLT),
            SLTIU   -> Encode.encode_iu(AluSel.SLTU),

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

            BEQ     -> Encode.encode_brch(CmpSel. EQ),
            BNE     -> Encode.encode_brch(CmpSel. NE),
            BGE     -> Encode.encode_brch(CmpSel. GE),
            BGEU    -> Encode.encode_brch(CmpSel.GEU),
            BLT     -> Encode.encode_brch(CmpSel. LT),
            BLTU    -> Encode.encode_brch(CmpSel.LTU),

            CSRRW   -> Encode.encode_csr(CSRWSel.W ),
            CSRRS   -> Encode.encode_csr(CSRWSel.S ),
            CSRRC   -> Encode.encode_csr(CSRWSel.C ),
            CSRRWI  -> Encode.encode_csr(CSRWSel.WI),
            CSRRSI  -> Encode.encode_csr(CSRWSel.SI),
            CSRRCI  -> Encode.encode_csr(CSRWSel.CI),
            // CSRRW   -> Encode.encode_csr(AluSel.ASEL),
            // CSRRS   -> Encode.encode_csr(AluSel.  OR),
            // CSRRC   -> Encode.encode_csr(AluSel.  AN),
            // CSRRWI  -> Encode.encode_csr(AluSel.ASEL),
            // CSRRSI  -> Encode.encode_csr(AluSel.  OR),
            // CSRRCI  -> Encode.encode_csr(AluSel.  AN),

            AUIPC   -> Encode.encode(InstType.UA, AluSel. ADD, CmpSel.N, CSRWSel.W),
            LUI     -> Encode.encode(InstType.UL, AluSel.BSEL, CmpSel.N, CSRWSel.W),
            EBREAK  -> Encode.encode(InstType.EB, AluSel.   N, CmpSel.N, CSRWSel.W),
            ECALL   -> Encode.encode(InstType.EC, AluSel.   N, CmpSel.Y, CSRWSel.W),
            MRET    -> Encode.encode(InstType.MR, AluSel.BSEL, CmpSel.Y, CSRWSel.W)
        ),
        default = Encode.encode(InstType.IVD, AluSel.N, CmpSel.N, CSRWSel.W)
    )

    def decode(inst: UInt) : OP = {
        return new OP(decoder(inst, truthTable))
    }
}