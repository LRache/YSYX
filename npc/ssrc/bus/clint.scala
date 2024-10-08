package bus

import chisel3._

class Clint extends Module {
    val io = IO(new Bundle {
        val raddr = Input (Bool())
        val rdata = Output(UInt(32.W))
    })

    val counterLow  = RegInit(0.U(32.W))
    val counterHigh = RegInit((0xffffffffL).U(32.W))
    counterLow := counterLow + 1.U
    counterHigh := Mux(counterLow.orR.asBool, counterHigh, counterHigh + 1.U)

    io.rdata := Mux(io.raddr, counterHigh, counterLow)
}

// class Clint extends BlackBox {
//     val io = IO(new Bundle {
//         val clk   = Input (Clock())
//         val reset = Input (Reset())
//         val raddr = Input (Bool())
//         val rdata = Output(UInt(32.W))
//     })
// }
