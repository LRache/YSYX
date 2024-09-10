import chisel3._

import cpu.HCPU
import bus.AXI4SRAM

class Top extends Module {
    val sram = Module(new AXI4SRAM)
    val cpu = Module(new HCPU(BigInt(0x30000000)))
    cpu.io.master <> sram.io

    // Unused
    cpu.io.interrupt := false.B
    cpu.io.slave := DontCare
}

import circt.stage.ChiselStage

object Top extends App {
    (new ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new Top)))
}
