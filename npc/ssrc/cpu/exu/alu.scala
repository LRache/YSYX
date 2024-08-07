package cpu.exu;

import chisel3._
import chisel3.util.MuxLookup
import circt.stage.ChiselStage

object AluSel extends Enumeration {
    type AluSel = Value
    val ADD, SUB, AND, OR, XOR, SLL, SRL, SRA, SLT, SLTU, BSEL, N = Value
}

class Alu extends Module {
    val io = IO(new Bundle {
        val a = Input(UInt(32.W))
        val b = Input(UInt(32.W))
        val sel = Input(UInt(4.W))
        val result = Output(UInt(32.W))
    })
    val signed_a = io.a.asSInt
    val signed_b = io.b.asSInt
    val shift = io.b(4,0)

    val resultSeq = Seq(
        (AluSel. ADD.id.U) -> (io.a + io.b),
        (AluSel. SUB.id.U) -> (io.a - io.b),
        (AluSel. AND.id.U) -> (io.a & io.b),
        (AluSel.  OR.id.U) -> (io.a | io.b),
        (AluSel. XOR.id.U) -> (io.a ^ io.b),
        (AluSel. SLL.id.U) -> (io.a << shift),
        (AluSel. SRL.id.U) -> (io.a >> shift),
        (AluSel. SRA.id.U) -> ((signed_a >> shift).asUInt),
        (AluSel. SLT.id.U) -> (signed_a < signed_b).asUInt,
        (AluSel.SLTU.id.U) -> (io.a < io.b).asUInt,
        (AluSel.BSEL.id.U) -> io.b,
    )

    io.result := MuxLookup(io.sel, 0.U(32.W))(resultSeq)
}

object Alu extends App {
    (new ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new Alu)))
}
