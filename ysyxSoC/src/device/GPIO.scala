package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class GPIOIO extends Bundle {
  val out = Output(UInt(16.W))
  val in = Input(UInt(16.W))
  val seg = Output(Vec(8, UInt(8.W)))
}

class GPIOCtrlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
  val gpio = new GPIOIO
}

class gpio_top_apb extends BlackBox {
  val io = IO(new GPIOCtrlIO)
}

class gpioChisel extends Module {
  val io = IO(new GPIOCtrlIO)
  
  val led = RegInit(0.U(16.W))
  val seg = RegInit(0.U(32.W))
  val ready = RegInit(false.B)
  val wen = RegInit(false.B)
  val ren = RegInit(false.B)
  val wmask = RegInit(0.U(4.W))
  val wdata = RegInit(0.U(32.W))

  wen := io.in.psel && io.in.penable &&  io.in.pwrite
  ren := io.in.psel && io.in.penable && !io.in.pwrite
  wmask := io.in.pstrb
  wdata := io.in.pwdata
  ready := Mux(ren && (io.in.paddr & 0xf.U) === 0x4.U, true.B, false.B)

  led := Mux(
    wen && ((io.in.paddr & 0xf.U) === 0x0.U), 
    MuxLookup(wmask, led)(Seq (
      0x1.U -> Cat(led  (15, 8), wdata(7, 0)),
      0x2.U -> Cat(wdata(15, 8), led  (7, 0)),
      0x3.U -> wdata(15, 0),
      0x5.U -> Cat(led  (15, 8), wdata(7, 0)),
      0x6.U -> Cat(wdata(15, 8), led  (7, 0)),
      0x7.U -> wdata(15,0),
      0x9.U -> Cat(led  (15, 8), wdata(7, 0)),
      0xa.U -> Cat(led  (15, 8), wdata(7, 0)),
      0xb.U -> wdata(15, 0),
      0xd.U -> Cat(led  (15, 8), wdata(7, 0)),
      0xe.U -> Cat(wdata(15, 8), led  (7, 0)),
      0xf.U -> wdata(15, 0)
    )),
    led)
  seg := Mux(
    wen && ((io.in.paddr & 0xf.U) === 0x8.U), 
    MuxLookup(wmask, seg)(Seq (
      0x1.U -> Cat(seg(31, 8), wdata(7, 0)),
      0x2.U -> Cat(seg(7, 0), wdata(15, 8), seg(7, 0)),
      0x3.U -> Cat(seg(31, 16), wdata(15, 8)),
      0x4.U -> Cat(seg(31, 24), wdata(23, 16), seg(15, 0)),
      0x5.U -> Cat(seg(31, 24), wdata(23, 16), seg(15, 8), wdata(7, 0)),
      0x6.U -> Cat(seg(31, 24), wdata(23, 8), seg(7, 0)),
      0x7.U -> Cat(seg(31, 24), wdata(23, 0)),
      0x8.U -> Cat(wdata(31, 24), seg(23, 0)),
      0x9.U -> Cat(wdata(31, 24), seg(23, 8), wdata(7, 0)),
      0xa.U -> Cat(wdata(31, 24), seg(23, 16), wdata(15, 8), seg(7, 0)),
      0xb.U -> Cat(wdata(31, 24), seg(23, 16), wdata(15, 0)),
      0xd.U -> Cat(wdata(31, 16), seg(15, 0)),
      0xe.U -> Cat(wdata(31, 16), seg(15, 8), wdata(7, 0)),
      0xf.U -> wdata
    )),
    seg)

  // io.in.prdata := Mux(ren && ((io.in.paddr & 0xf.U) === 0x4.U), io.gpio.in, 0.U)
  io.in.prdata := io.gpio.in
  io.in.pready := wen || ren
  io.in.pslverr := false.B
  io.gpio.out := led

  def seg_mux(s: UInt) : UInt = {
    MuxLookup(s, 0.U)(Seq(
      0x0.U -> 0x40.U,
      0x1.U -> 0x79.U,
      0x2.U -> 0x24.U,
      0x3.U -> 0x30.U,
      0x4.U -> 0x19.U,
      0x5.U -> 0x12.U,
      0x6.U -> 0x02.U,
      0x7.U -> 0x78.U,
      0x8.U -> 0x00.U,
      0x9.U -> 0x10.U,
      0xa.U -> 0x08.U,
      0xb.U -> 0x03.U,
      0xc.U -> 0x46.U,
      0xd.U -> 0x21.U,
      0xe.U -> 0x06.U,
      0xf.U -> 0x0e.U
    ))
  }

  io.gpio.seg(0) := seg_mux(seg( 3, 0))
  io.gpio.seg(1) := seg_mux(seg( 7, 4))
  io.gpio.seg(2) := seg_mux(seg(11, 8))
  io.gpio.seg(3) := seg_mux(seg(15,12))
  io.gpio.seg(4) := seg_mux(seg(19,16))
  io.gpio.seg(5) := seg_mux(seg(23,20))
  io.gpio.seg(6) := seg_mux(seg(27,24))
  io.gpio.seg(7) := seg_mux(seg(31,28))
}

class APBGPIO(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val gpio_bundle = IO(new GPIOIO)

    val mgpio = Module(new gpioChisel)
    mgpio.io.clock := clock
    mgpio.io.reset := reset
    mgpio.io.in <> in
    gpio_bundle <> mgpio.io.gpio
  }
}
