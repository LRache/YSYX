package ysyx

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import freechips.rocketchip.diplomacy.BindingScope.add

class QSPIIO extends Bundle {
  val sck = Output(Bool())
  val ce_n = Output(Bool())
  val dio = Analog(4.W)
}

class psram_top_apb extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    val qspi = new QSPIIO
  })
}

class psram extends BlackBox {
  val io = IO(Flipped(new QSPIIO))
}

class PSRAMBase extends BlackBox {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val addr = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val count = Input(UInt(5.W))
    val wen = Input(Bool())
    val ren = Input(Bool())
    val rdata = Output(UInt(32.W))
  })
}

class psramChisel extends RawModule {
  val io = IO(Flipped(new QSPIIO))

  val s_cmd :: s_addr :: s_send :: s_recv :: s_inv :: nil = Enum(5)

  withClockAndReset(io.sck.asClock, io.ce_n) {
    val rdata = RegInit(0.U(4.W))
    val di = TriStateInBuf(io.dio, rdata, false.B) // change this if you need
    val psram = Module(new PSRAMBase)
    val state = RegInit(s_cmd)
    val counter = RegInit(0.U(8.W))
    val cmd = RegInit(0.U(8.W))
    val addr = RegInit(0.U(24.W))

    psram.io.clk := io.sck.asClock
    psram.io.wen := state === s_recv && ~io.ce_n
    psram.io.ren := state === s_send && ~io.ce_n
    psram.io.addr := Cat(0.U(8.W), addr)
    psram.io.count := counter
    psram.io.wdata := Mux(state === s_recv, Cat(di(3), di(2), di(1), di(0)), 0.U)

    when (state === s_cmd) {
      cmd     := cmd.bitSet(7.U - counter, di(0))
      state   := Mux(counter === 7.U, s_addr, s_cmd)
      counter := Mux(counter === 7.U, 0.U, counter + 1.U)
    } .elsewhen(state === s_addr) {
      addr := addr.bitSet(23.U - counter, di(3)).bitSet(22.U - counter, di(2)).bitSet(21.U - counter, di(1)).bitSet(20.U - counter, di(0))
      counter := Mux(counter === 20.U, 0.U, counter + 4.U)
      state   := Mux(counter === 20.U, MuxLookup(cmd, s_inv)(Seq (
          0x38.U -> s_recv,
          0xeb.U -> s_send
      )), s_addr)
    } .elsewhen(state === s_recv) {
      counter := counter + 1.U
      printf("%d\n", counter)
    } .elsewhen(state === s_send) {
      rdata := rdata.
      bitSet(0.U, psram.io.rdata(20.U - counter)).
      bitSet(1.U, psram.io.rdata(21.U - counter)).
      bitSet(2.U, psram.io.rdata(22.U - counter)).
      bitSet(3.U, psram.io.rdata(23.U - counter))
      counter := counter + 4.U
    }
    assert(state =/= s_inv, "Invalid cmd %x", cmd)
  }

}

class APBPSRAM(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val qspi_bundle = IO(new QSPIIO)

    val mpsram = Module(new psram_top_apb)
    mpsram.io.clock := clock
    mpsram.io.reset := reset
    mpsram.io.in <> in
    qspi_bundle <> mpsram.io.qspi
  }
}
