package ysyx

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.amba._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class AXI4DelayerIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val in = Flipped(new AXI4Bundle(AXI4BundleParameters(addrBits = 32, dataBits = 32, idBits = 4)))
  val out = new AXI4Bundle(AXI4BundleParameters(addrBits = 32, dataBits = 32, idBits = 4))
}

class axi4_delayer extends BlackBox {
  val io = IO(new AXI4DelayerIO)
}

class AXI4DelayerChisel extends Module {
    val io = IO(new AXI4DelayerIO)
    if (Config.hasDelay) {
        val s = 1
        val rs = 7

        val s_wait_slave :: s_wait_master :: s_save_data :: s_delay :: s_valid :: Nil = Enum(5)
        val rstate = RegInit(s_wait_slave)
        val wstate = RegInit(s_wait_slave)

        val rCounter = RegInit(0.U(32.W))
        val wCounter = RegInit(0.U(32.W))
        
        val rSlaveDone = io.in.ar.valid && io.out.ar.ready
        val wSlaveDone = io.in.w.valid && io.in.w.ready
        // val wMasterDone = io.out.b.ready && io.out.b.valid
        
        val bresp = RegInit(0.U(2.W))
        val rresp = RegInit(0.U(2.W))

        val rBurstCounter = RegInit(0.U(3.W))
        rBurstCounter := MuxLookup(rstate, rBurstCounter) (Seq(
            s_wait_slave -> 0.U,
            // s_wait_master -> 0.U,
            s_wait_master -> Mux(io.out.r.valid, 1.U, 0.U),
            s_save_data -> Mux(io.out.r.valid, rBurstCounter + 1.U, rBurstCounter),
            // s_valid -> Mux(io.in.r.ready, rBurstCounter - 1.U, rBurstCounter)
        ))
        val rOutBurstCounter = RegInit(0.U(3.W))
        rOutBurstCounter := Mux(rstate === s_valid, rOutBurstCounter + 1.U, 0.U)

        // val rdataArray = VecInit(Seq.fill(8)(RegEnable(io.out.r.bits.data, 0.U, io.out.r.valid)))
        val rdataArray = VecInit((0 to 7).map(i => RegEnable(io.out.r.bits.data, 0x12345678.U, io.out.r.valid && (rBurstCounter === i.U))))
        val rdata = RegInit(0.U(32.W))
        rdata := Mux(io.out.r.valid, io.out.r.bits.data, rdata)
        
        rresp := Mux(io.out.r.valid, io.out.r.bits.resp, rresp)

        rstate := MuxLookup(rstate, s_wait_slave) (Seq(
            s_wait_slave  -> Mux(rSlaveDone, s_wait_master, s_wait_slave),
            s_wait_master -> Mux(io.out.r.valid, Mux(io.out.r.bits.last, s_delay, s_save_data), s_wait_master),
            s_save_data   -> Mux(io.out.r.bits.last && io.out.r.valid, s_delay, s_save_data),
            s_delay -> Mux(rCounter + (2*s).U < s.U, s_valid, s_delay),
            s_valid -> Mux(rOutBurstCounter + 1.U === rBurstCounter, s_wait_slave, s_valid)
        ))
        // when (rstate === s_wait_master && (io.out.r.valid)) {
        //     printf("s_wait_master: rBurstCounter = %d data = %x\n", rBurstCounter, io.out.r.bits.data)
        // }
        // when (rstate === s_save_data && (io.out.r.valid)) {
        //     printf("s_save_data: rBurstCounter = %d data= %x\n", rBurstCounter, io.out.r.bits.data)
        // }
        // when (rstate === s_valid) {
        //     printf("s_valid: rBurstCounter = %d rOutBurstCounter = %d data = %x last = %d\n", rBurstCounter, rOutBurstCounter, rdataArray(rOutBurstCounter), io.in.r.bits.last)
        // }
        // when (io.out.r.valid) {
        //     printf("valid rBurstCounter = %d rOutBurstCounter = %d data = %x %d\n", rBurstCounter, rOutBurstCounter, io.out.r.bits.data, io.out.r.bits.last)
        // } .elsewhen(io.out.r.bits.last) {
        //     printf("last rBurstCounter = %d rOutBurstCounter = %d data = %x\n", rBurstCounter, rOutBurstCounter, io.out.r.bits.data)
        // }

        rCounter := MuxLookup(rstate, 0.U) (Seq(
            s_wait_master -> (rCounter + rs.U),
            s_delay -> (rCounter - s.U)
        ))

        bresp := Mux(io.out.b.valid, io.out.b.bits.resp, bresp)

        wstate := MuxLookup(wstate, s_wait_slave) (Seq(
            s_wait_slave -> Mux(wSlaveDone, s_wait_master, s_wait_slave),
            s_wait_master -> Mux(io.out.b.valid, s_delay, s_wait_master),
            s_delay -> Mux(wCounter + (2*s).U < s.U, s_valid, s_delay),
            s_valid -> Mux(io.out.b.ready, s_wait_slave, s_valid)
        ))

        wCounter := MuxLookup(wstate, 0.U) (Seq(
            s_wait_master -> (wCounter + rs.U),
            s_delay -> (wCounter - s.U)
        ))

        io.out.ar.valid := io. in.ar.valid
        io. in.ar.ready := io.out.ar.ready
        io.out.ar.bits.addr := io.in.ar.bits.addr
        io.out.ar.bits.size := io.in.ar.bits.size
        io.out.ar.bits.len := io.in.ar.bits.len
        io.out.ar.bits.burst := io.in.ar.bits.burst
        io.out.ar.bits.id := io.in.ar.bits.id
        io.out.ar.bits.lock := io.in.ar.bits.lock
        io.out.ar.bits.cache := io.in.ar.bits.cache
        io.out.ar.bits.prot := io.in.ar.bits.prot
        io.out.ar.bits.qos := io.in.ar.bits.qos

        io.out.r.ready := io. in.r.ready
        io. in.r.valid := rstate === s_valid
        io. in.r.bits.data := rdataArray(rOutBurstCounter)
        io. in.r.bits.resp := rresp
        io. in.r.bits.last := (rOutBurstCounter + 1.U) === rBurstCounter && rstate === s_valid
        io. in.r.bits.id := io.out.r.bits.id

        io.out.aw.valid := io. in.aw.valid
        io. in.aw.ready := io.out.aw.ready
        io.out.aw.bits.addr := io.in.aw.bits.addr
        io.out.aw.bits.size := io.in.aw.bits.size
        io.out.aw.bits.len := io.in.aw.bits.len
        io.out.aw.bits.burst := io.in.aw.bits.burst
        io.out.aw.bits.id := io.in.aw.bits.id
        io.out.aw.bits.lock := io.in.aw.bits.lock
        io.out.aw.bits.cache := io.in.aw.bits.cache
        io.out.aw.bits.prot := io.in.aw.bits.prot
        io.out.aw.bits.qos := io.in.aw.bits.qos

        io.out.w.valid := io. in.w.valid
        io. in.w.ready := io.out.w.ready
        io.out.w.bits.data := io.in.w.bits.data
        io.out.w.bits.strb := io.in.w.bits.strb
        io.out.w.bits.last := io.in.w.bits.last

        io.out.b.ready := io. in.b.ready
        io. in.b.valid := wstate === s_valid
        io. in.b.bits.resp := bresp
        io. in.b.bits.id := io.out.b.bits.id
    } else {
        io.out <> io.in
    }
}

class AXI4DelayerWrapper(implicit p: Parameters) extends LazyModule {
  val node = AXI4IdentityNode()

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, edgeIn), (out, edgeOut)) =>
      val delayer = Module(new AXI4DelayerChisel)
      delayer.io.clock := clock
      delayer.io.reset := reset
      delayer.io.in <> in
      out <> delayer.io.out
    }
  }
}

object AXI4Delayer {
  def apply()(implicit p: Parameters): AXI4Node = {
    val axi4delay = LazyModule(new AXI4DelayerWrapper)
    axi4delay.node
  }
}
