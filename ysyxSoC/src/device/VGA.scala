package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class VGAIO extends Bundle {
  val r = Output(UInt(8.W))
  val g = Output(UInt(8.W))
  val b = Output(UInt(8.W))
  val hsync = Output(Bool())
  val vsync = Output(Bool())
  val valid = Output(Bool())
}

class VGACtrlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Bool())
  val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
  val vga = new VGAIO
}

class vga_top_apb extends BlackBox {
  val io = IO(new VGACtrlIO)
}

class VGABuffer extends BlackBox {
  val io = IO(new Bundle {
    val waddr = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val wmask = Input(UInt(4.W))
    val wen = Input(Bool())

    val rx = Input(UInt(32.W))
    val ry = Input(UInt(32.W))
    val rdata = Output(UInt(32.W))
    val ren = Input(Bool())
  })
}

class vgaChisel extends Module {
  val xFrontporch = 96.U
  val xActive = 144.U
  val xBackporch = 784.U
  val xTotal = 799.U
  val yFrontporch = 2.U
  val yActive = 35.U
  val yBackporch = 515.U
  val yTotal = 524.U

  val io = IO(new VGACtrlIO)

  val buffer = Module(new VGABuffer)
  buffer.io.waddr := io.in.paddr
  buffer.io.wdata := io.in.pwdata
  buffer.io.wmask := io.in.pstrb
  buffer.io.wen := io.in.psel && io.in.penable && io.in.pwrite
  io.in.pready := true.B
  io.in.prdata := 0.B
  io.in.pslverr := false.B
  
  val x = RegInit(0.U(10.W))
  val y = RegInit(0.U(10.W))

  val xValid = x > xActive && x <= xBackporch
  val yValid = y > yActive && y <= yBackporch
  val valid = xValid && yValid
  val hs = x >= xFrontporch
  val vs = y >= yFrontporch

  buffer.io.rx := x - xActive - 1.U
  buffer.io.ry := y - yActive - 1.U
  buffer.io.ren := valid
  val color = buffer.io.rdata

  x := Mux(x === xTotal, 0.U, x + 1.U)
  y := Mux(y === yTotal && x ===xTotal, 0.U, Mux(x === xTotal, y + 1.U, y))

  io.vga.valid := valid
  io.vga.hsync := hs
  io.vga.vsync := vs
  io.vga.r := Mux(valid, color( 7,  0), 0.U)
  io.vga.g := Mux(valid, color(15,  8), 0.U)
  io.vga.b := Mux(valid, color(23, 16), 0.U)
}

class APBVGA(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
  val node = APBSlaveNode(Seq(APBSlavePortParameters(
    Seq(APBSlaveParameters(
      address       = address,
      executable    = true,
      supportsRead  = true,
      supportsWrite = true)),
    beatBytes  = 4)))

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)
    val vga_bundle = IO(new VGAIO)

    val mvga = Module(new vgaChisel)
    mvga.io.clock := clock
    mvga.io.reset := reset
    mvga.io.in <> in
    vga_bundle <> mvga.io.vga
  }
}
