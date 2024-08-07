package bus

import chisel3._

class Memory extends BlackBox {
    val io = IO(new Bundle {
        val waddr   = Input (UInt(32.W))
        val wdata   = Input (UInt(32.W))
        val wmask   = Input (UInt(8.W))
        val wen     = Input (Bool())
        val raddr   = Input (UInt(32.W))
        val rdata   = Output(UInt(32.W))
        val rsize   = Input (UInt(3.W))
        val ren     = Input (Bool())

        val reset   = Input (Bool())
    })
}
