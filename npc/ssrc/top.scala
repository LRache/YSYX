import chisel3._

import cpu.HCPU
import bus.AXI4SRAM

class Top extends Module {
    val sram = Module(new AXI4SRAM)
    val cpu = Module(new HCPU)
    cpu.io.master <> sram.io

    // Unused
    cpu.io.interrupt := false.B
    cpu.io.slave.awvalid := 0.U
    cpu.io.slave.awaddr  := 0.U
    cpu.io.slave.awid    := 0.U
    cpu.io.slave.awlen   := 0.U
    cpu.io.slave.awsize  := 0.U
    cpu.io.slave.awburst := 0.U
    cpu.io.slave.wvalid  := 0.U
    cpu.io.slave.wdata   := 0.U
    cpu.io.slave.wstrb   := 0.U
    cpu.io.slave.wlast   := 0.U
    cpu.io.slave.bready  := 0.U
    cpu.io.slave.arvalid := 0.U
    cpu.io.slave.araddr  := 0.U
    cpu.io.slave.arid    := 0.U
    cpu.io.slave.arlen   := 0.U
    cpu.io.slave.arsize  := 0.U
    cpu.io.slave.arburst := 0.U
    cpu.io.slave.rready  := 0.U
}

import circt.stage.ChiselStage

object Top extends App {
    (new ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new Top)))
}
