package cpu.exu;

import chisel3._
import chisel3.util.MuxLookup
import circt.stage.ChiselStage
import cpu.Config

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

object AluInline {
    def apply(
        a: UInt, 
        b: UInt, 
        c: UInt, 
        d: UInt, 
        func3: UInt, 
        addT: Bool, 
        tag: Bool, 
        res: UInt, 
        cmp: Bool, 
        csr: UInt
    ): Unit = {
        val sa = a.asSInt
        val ua = a.asUInt
        val sb = b.asSInt
        val ub = b.asUInt
        val sc = c.asSInt
        val uc = c.asUInt
        val sd = d.asSInt
        val ud = d.asUInt
        val shift = b(4,0)

        val add = Wire(UInt(32.W))
        if (Config.HasFastAlu) {
            add := Mux(tag, sa - sb, sa + sb).asUInt
        } else {
            add := (sa + Mux(tag, -sb, sb)).asUInt
        }
        val lt  = sc < sd
        val ltu = uc < ud
        val or  = ua | ub
        val xor = uc ^ ud
        val sr  = ua >> shift
        val eq  = !xor.orR

        val resultTable = Seq(
            AluFunc3. ADD -> add,
            AluFunc3. AND -> (ua & ub),
            AluFunc3.  OR -> or,
            AluFunc3. XOR -> xor,
            AluFunc3. SLL -> (ua << shift),
            AluFunc3.  SR -> Mux(tag, ua >> shift, (sa >> shift).asUInt),
            AluFunc3. SLT -> lt .asUInt,
            AluFunc3.SLTU -> ltu.asUInt,
        )
        res := Mux(addT, add, MuxLookup(func3, 0.U(32.W))(resultTable))

        val t = Mux(
            func3(2),
            Mux(
                func3(1),
                ltu,
                lt
            ),
            eq
        )
        cmp := Mux(func3(0), !t, t)

        csr := MuxLookup(func3(1,0), 0.U(32.W))(Seq (
            0.U -> ua,
            1.U -> ub,
            2.U -> or,
            3.U -> (ua & ~ub)
        ))
    }
}

class Alu extends Module {
    val io = IO(new Bundle {
        val a     = Input(UInt(32.W))
        val b     = Input(UInt(32.W))
        val c     = Input(UInt(32.W))
        val d     = Input(UInt(32.W))
        val func3 = Input(UInt(3.W))
        val addT  = Input(Bool())     // select a plus b anyway
        val tag   = Input(Bool())     // for sub or unsinged operation
        
        val res = Output(UInt(32.W))  // alu result
        val cmp = Output(Bool())      // cmp result
        val csr = Output(UInt(32.W))  // csr result
    })
    AluInline(
        io.a,
        io.b,
        io.c,
        io.d,
        io.func3,
        io.addT,
        io.tag,
        io.res,
        io.cmp,
        io.csr
    )
}

object Alu extends App {
    (new ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new Alu)))
}
