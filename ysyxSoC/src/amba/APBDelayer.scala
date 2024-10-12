package ysyx

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.amba._
import freechips.rocketchip.amba.apb._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class APBDelayerIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
  val out = new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32))
}

class apb_delayer extends BlackBox {
  val io = IO(new APBDelayerIO)
}

class APBDelayerChisel extends Module {
  val io = IO(new APBDelayerIO)

  if (Config.hasDelay) {
    val rs = 7
    val s = 1
    
    io.out.pwrite := io.in.pwrite
    io.out.paddr := io.in.paddr
    io.out.pprot := io.in.pprot
    io.out.pwdata := io.in.pwdata
    io.out.pstrb := io.in.pstrb

    val slverr = RegInit(false.B)
    val rdata = RegInit(0.U(32.W))

    val s_wait_enable :: s_wait_ready :: s_delay :: s_ready :: Nil = Enum(4)
    val state = RegInit(s_wait_enable)
    val counter = RegInit(0.U(32.W))
    counter := MuxLookup(state, 0.U) (Seq(
      s_wait_ready  -> (counter + rs.U),
      s_delay       -> (counter - s.U)
    ))
    state := MuxLookup(state, s_wait_enable) (Seq (
      s_wait_enable -> Mux(io.in.penable, s_wait_ready, s_wait_enable),
      s_wait_ready  -> Mux(io.out.pready, s_delay, s_wait_ready),
      s_delay       -> Mux(counter - (2*s).U < s.U, s_ready, s_delay),
      s_ready       -> s_wait_enable,
    ))

    val ready = state === s_ready
    slverr := Mux(io.out.pready, io.out.pslverr, slverr)
    rdata := Mux(io.out.pready, io.out.prdata, rdata)

    io.in.pready := ready
    io.in.pslverr := Mux(ready, slverr, false.B)
    io.in.prdata := Mux(ready, rdata, 0.U)
    io.out.psel := Mux(state === s_wait_enable || state === s_wait_ready, io.in.psel, false.B)
    io.out.penable := Mux(state === s_wait_enable || state === s_wait_ready, io.in.penable, false.B)
  } else {
    io.out <> io.in
  }
}

class APBDelayerWrapper(implicit p: Parameters) extends LazyModule {
  val node = APBIdentityNode()

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, edgeIn), (out, edgeOut)) =>
      val delayer = Module(new APBDelayerChisel)
      delayer.io.clock := clock
      delayer.io.reset := reset
      delayer.io.in <> in
      out <> delayer.io.out
    }
  }
}

object APBDelayer {
  def apply()(implicit p: Parameters): APBNode = {
    val apbdelay = LazyModule(new APBDelayerWrapper)
    apbdelay.node
  }
}
