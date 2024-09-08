package cpu.exu;

import chisel3._
import chisel3.util.MuxLookup
import circt.stage.ChiselStage

// object AluSel extends Enumeration {
//     type AluSel = Value
//     val ADD, SUB, AND, OR, XOR, SLL, SRL, SRA, SLT, SLTU, ASEL, BSEL, AN, N = Value
// }
object AluSel extends Enumeration{
    type AluSel = Value
    val ADD, BSEL, FUNCT3 = Value
}

object AluFunc3 {
    val ADD  = 0.U(3.W)
    val XOR  = 4.U(3.W)
    val OR   = 6.U(3.W)
    val AND  = 7.U(3.W)
    val SLL  = 1.U(3.W)
    val SR   = 5.U(3.W)
    val SLT  = 2.U(3.W)
    val SLTU = 3.U(3.W)
}

class Alu extends Module {
    val io = IO(new Bundle {
        val a = Input(UInt(32.W))
        val b = Input(UInt(32.W))
        val func3 = Input(UInt(3.W))
        val tag = Input(Bool())
        val result = Output(UInt(32.W))
    })
    val a = io.a
    val b = io.b
    val signed_a = a.asSInt
    val signed_b = b.asSInt
    val neg_b = -io.b
    val shift = b(4,0)
    val tag = io.tag

    // val resultTable = Seq(
    //     (AluSel. ADD.id.U) -> (io.a + io.b),
    //     (AluSel. SUB.id.U) -> (io.a - io.b),
    //     (AluSel. AND.id.U) -> (io.a & io.b),
    //     (AluSel.  OR.id.U) -> (io.a | io.b),
    //     (AluSel. XOR.id.U) -> (io.a ^ io.b),
    //     (AluSel. SLL.id.U) -> (io.a << shift),
    //     (AluSel. SRL.id.U) -> (io.a >> shift),
    //     (AluSel. SRA.id.U) -> ((signed_a >> shift).asUInt),
    //     (AluSel. SLT.id.U) -> (signed_a < signed_b).asUInt,
    //     (AluSel.SLTU.id.U) -> (io.a < io.b).asUInt,
    //     (AluSel.BSEL.id.U) -> io.b,
    // )
    val resultTable = Seq(
        AluFunc3. ADD -> (a + Mux(tag, neg_b, b)),
        AluFunc3. AND -> (io.a & io.b),
        AluFunc3.  OR -> (io.a | io.b),
        AluFunc3. XOR -> (io.a ^ io.b),
        AluFunc3. SLL -> (io.a << shift),
        AluFunc3.  SR -> Mux(tag, (signed_a >> shift).asUInt, io.a >> shift),
        AluFunc3. SLT -> (signed_a < signed_b).asUInt,
        AluFunc3.SLTU -> (io.a < io.b).asUInt,
    )
    io.result := MuxLookup(io.func3, 0.U(32.W))(resultTable)
}

object Alu extends App {
    (new ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new Alu)))
}
