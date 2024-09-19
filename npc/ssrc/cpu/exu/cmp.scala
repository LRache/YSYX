package cpu.exu

import chisel3._
import chisel3.util._

object CmpSel{
    val EQ = 0b000.U(3.W)
    val NE = 0b001.U(3.W)
    val LT = 0b100.U(3.W)
    val GE = 0b101.U(3.W)
    val BLTU = 0b110.U(3.W)
    val BGEU = 0b111.U(3.W)
}

class Cmp extends Module {
    val io = IO(new Bundle {
        val a     = Input (UInt(32.W))
        val b     = Input (UInt(32.W))
        val func3 = Input (UInt(3.W))
        val res   = Output(Bool())
    })
    val func3 = io.func3
    val sa = io.a.asSInt
    val sb = io.b.asSInt
    val ua = io.a.asUInt
    val ub = io.b.asUInt

    val eq = ua === ub
    val lt = sa < sb
    val ltu = ua < ub
    val t = Mux(
        func3(2),
        Mux(
            func3(1),
            ltu,
            lt
        ),
        eq
    )
    io.res := Mux(func3(0), !t, t)
}
