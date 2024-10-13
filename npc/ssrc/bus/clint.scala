package bus

import chisel3._

object ClintInline {
    def apply(raddr: Bool, rdata: UInt): Unit = {
        val counterLow  = RegInit(0.U(32.W))
        val counterHigh = RegInit((0xffffffffL).U(32.W))
        counterLow := counterLow + 1.U
        counterHigh := Mux(counterLow.orR.asBool, counterHigh, counterHigh + 1.U)
        rdata := Mux(raddr, counterHigh, counterLow)
    }
}

class Clint extends Module {
    val io = IO(new Bundle {
        val raddr = Input (Bool())
        val rdata = Output(UInt(32.W))
    })

    ClintInline(
        raddr = io.raddr,
        rdata = io.rdata
    )
}
