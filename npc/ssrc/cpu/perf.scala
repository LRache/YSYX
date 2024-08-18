package cpu

import chisel3._

class PerfCounter extends BlackBox {
    val io = IO(new Bundle {
        val ifu_valid = Input(Bool())
    })
}
