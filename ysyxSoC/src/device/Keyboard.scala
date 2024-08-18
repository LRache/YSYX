package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class PS2IO extends Bundle {
  val clk = Input(Bool())
  val data = Input(Bool())
}

class PS2CtrlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Bool())
  val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
  val ps2 = new PS2IO
}

class ps2_top_apb extends BlackBox {
  val io = IO(new PS2CtrlIO)
}

class ps2Chisel extends Module {
  val io = IO(new PS2CtrlIO)

  val ps2ClkLast = RegInit(false.B)
  ps2ClkLast := io.ps2.clk
  val sampling = ps2ClkLast && !io.ps2.clk

  val is_read = io.in.psel && io.in.penable && !io.in.pwrite && !((io.in.paddr & 0xf.U).orR)

  val queue = Module(new Queue(UInt(8.W), 32))
  val buffer = RegInit(0.U(10.W))
  val state = RegInit(0.U(4.W))
  val qin = RegInit(0.U(8.W))
  val ren = RegInit(false.B)
  val rdata = RegInit(0.U(8.W))
  ren := is_read
  rdata := Mux(queue.io.deq.valid, queue.io.deq.bits, 0.U)
  
  val bufferValid = state === 10.U && sampling && !buffer(0) && io.ps2.data && buffer(9,1).xorR // valid at state=10
  state   := Mux(sampling, Mux(state === 10.U, 0.U, state + 1.U), state)
  buffer  := Mux(sampling, Mux(state === 10.U, 0.U, buffer.bitSet(state, io.ps2.data)), buffer)
  qin     := buffer(8,1)
  queue.io.enq.bits := qin
  queue.io.enq.valid := bufferValid
  
  io.in.pready  := true.B
  io.in.prdata  := rdata
  queue.io.deq.ready := is_read
  io.in.pslverr := false.B
}

class APBKeyboard(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val ps2_bundle = IO(new PS2IO)

    val mps2 = Module(new ps2Chisel)
    mps2.io.clock := clock
    mps2.io.reset := reset
    mps2.io.in <> in
    ps2_bundle <> mps2.io.ps2
  }
}
