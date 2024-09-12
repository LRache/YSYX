package cpu.exu;

import chisel3._
import chisel3.util.MuxLookup
import circt.stage.ChiselStage

// object AluSel extends Enumeration {
//     type AluSel = Value
//     val ADD, SUB, AND, OR, XOR, SLL, SRL, SRA, SLT, SLTU, ASEL, BSEL, AN, N = Value
// }

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
        val cmp = Output(Bool())
        // val csr = Output(UInt(32.W))
    })
    val func3 = io.func3
    val sa = io.a.asSInt
    val sb = io.b.asSInt
    val ua = io.a.asUInt
    val ub = io.b.asUInt
    val shift = io.b(4,0)
    val tag = io.tag

    val lt  = sa < sb
    val ltu = ua < ub
    val or  = ua | ub
    val xor = ua ^ ub
    val eq  = !xor.orR

    val resultTable = Seq(
        AluFunc3. ADD -> (sa + Mux(io.tag, -sb, sb)).asUInt,
        AluFunc3. AND -> (ua & ub),
        AluFunc3.  OR -> or,
        AluFunc3. XOR -> xor,
        AluFunc3. SLL -> (ua << shift),
        AluFunc3.  SR -> Mux(tag, ua >> shift, (sa >> shift).asUInt),
        AluFunc3. SLT -> lt.asUInt,
        AluFunc3.SLTU -> ltu.asUInt,
    )
    io.result := MuxLookup(func3, 0.U(32.W))(resultTable)

    val t = Mux(
        func3(2),
        Mux(
            func3(1),
            ltu,
            lt
        ),
        eq
    )
    io.cmp := Mux(func3(0), !t, t)

    // io.csr := MuxLookup(func3(1,0), 0.U(32.W))(Seq (
    //         1.U -> ub,
    //         2.U -> or,
    //         3.U -> (ua & ub)
    // ))
}

object Alu extends App {
    (new ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new Alu)))
}
