package cpu.exu

import chisel3._
import chisel3.util._

object CmpSel extends Enumeration {
    type CmpSel = Value
    val EQ, NE, GE, GEU, LT, LTU, N, Y = Value
}

class Cmp extends Module {
    val io = IO(new Bundle {
        val sel = Input (UInt(3.W))
        val a   = Input (UInt(32.W))
        val b   = Input (UInt(32.W))
        val res = Output(Bool())
    })
    val signed_a = io.a.asSInt
    val signed_b = io.b.asSInt
    
    val cmp_eq  = io.a === io.b
    val cmp_ne  = !cmp_eq
    val cmp_lt  = signed_a < signed_b
    val cmp_ge  = !cmp_lt
    val cmp_ltu = io.a < io.b
    val cmp_geu = !cmp_ltu

    io.res := MuxLookup(io.sel, 1.B)(Seq(
        CmpSel. EQ.id.U -> cmp_eq,
        CmpSel. NE.id.U -> cmp_ne,
        CmpSel. LT.id.U -> cmp_lt,
        CmpSel.LTU.id.U -> cmp_ltu,
        CmpSel. GE.id.U -> cmp_ge,
        CmpSel.GEU.id.U -> cmp_geu,
        CmpSel.  N.id.U -> 0.B,
        CmpSel.  Y.id.U -> 1.B
    ))
}
