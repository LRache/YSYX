package cpu.exu;

import chisel3._
import chisel3.util.MuxLookup
import circt.stage.ChiselStage
import cpu.Config
import cpu.idu.Func3UInt

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
            Func3UInt. ADD -> add,
            Func3UInt. AND -> (ua & ub),
            Func3UInt.  OR -> or,
            Func3UInt. XOR -> xor,
            Func3UInt. SLL -> (ua << shift),
            Func3UInt.  SR -> Mux(tag, ua >> shift, (sa >> shift).asUInt),
            Func3UInt. SLT -> lt .asUInt,
            Func3UInt.SLTU -> ltu.asUInt,
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
    val firtoolOptions = Array("--lowering-options=" + List(
        // make yosys happy
        // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
        "disallowLocalVariables",
        "disallowPackedArrays",
        "locationInfoStyle=wrapInAtSquareBracket"
    ).reduce(_ + "," + _))
    circt.stage.ChiselStage.emitSystemVerilogFile(new Alu, args, firtoolOptions)
}
