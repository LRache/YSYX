package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import scala.collection.Searching

class SPIIO(val ssWidth: Int = 8) extends Bundle {
  val sck = Output(Bool())
  val ss = Output(UInt(ssWidth.W))
  val mosi = Output(Bool())
  val miso = Input(Bool())
}

class spi_top_apb extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    val spi = new SPIIO
    val spi_irq_out = Output(Bool())
  })
}

class flash extends BlackBox {
  val io = IO(Flipped(new SPIIO(1)))
}

class APBSPI(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val spi_bundle = IO(new SPIIO)

    val mspi = Module(new spi_top_apb)
    mspi.io.clock := clock
    mspi.io.reset := reset
    spi_bundle <> mspi.io.spi

    val s_idle :: s_setTx_0 :: s_setTx_1 :: s_waitTx :: s_setDiv_0 :: s_setDiv_1 :: s_waitDiv :: s_setSS_0 :: s_setSS_1 :: s_waitSS ::s_setCTL_0 :: s_setCTL_1 :: s_waitCTL :: s_readCTL_0 :: s_readCTL_1 :: s_waitData :: s_readRx_0 :: s_readRx_1 :: s_waitRx :: Nil = Enum(19);
    val state = RegInit(s_idle);
    val read_flash = in.paddr(29) & in.penable;

    assert(state != s_idle)
    state := MuxLookup(state, s_idle)(Seq (
      s_idle     -> Mux(read_flash, s_setDiv_0, s_idle),
      
      s_setTx_0  -> s_setTx_1,
      s_setTx_1  -> s_waitTx,
      s_waitTx   -> Mux(mspi.io.in.pready, s_setSS_0, s_waitTx),

      s_setDiv_0 -> s_setDiv_1,
      s_setDiv_1 -> s_waitDiv,
      s_waitDiv  -> Mux(mspi.io.in.pready, s_setTx_0, s_waitDiv),

      s_setSS_0  -> s_setSS_1,
      s_setSS_1  -> s_waitSS,
      s_waitSS   -> Mux(mspi.io.in.pready, s_setCTL_0, s_waitSS),

      s_setCTL_0 -> s_setCTL_1,
      s_setCTL_1 -> s_waitCTL,
      s_waitCTL  -> Mux(mspi.io.in.pready, s_readCTL_0, s_waitCTL),

      s_readCTL_0-> s_readCTL_1,
      s_readCTL_1-> s_waitData,
      s_waitData -> Mux(mspi.io.in.pready & !mspi.io.in.prdata(8), s_readRx_0, s_readCTL_0),

      s_readRx_0 -> s_readRx_1,
      s_readRx_1 -> s_waitRx,
      s_waitRx   -> Mux(mspi.io.in.pready, s_idle, s_waitRx)
    ))
    
    mspi.io.in.paddr := MuxLookup(state, in.paddr)(Seq (
      s_idle      -> in.paddr,
      s_setTx_0   -> 0x10001004.U,
      s_setTx_1   -> 0x10001004.U,
      s_waitTx    -> 0x10001004.U,
      
      s_setDiv_0  -> 0x10001014.U,
      s_setDiv_1  -> 0x10001014.U,
      s_waitDiv   -> 0x10001014.U,
      
      s_setSS_0   -> 0x10001018.U,
      s_setSS_1   -> 0x10001018.U,
      s_waitSS    -> 0x10001018.U,
      
      s_setCTL_0  -> 0x10001010.U,
      s_setCTL_1  -> 0x10001010.U,
      s_waitCTL   -> 0x10001010.U,
      
      s_readCTL_0 -> 0x10001010.U,
      s_readCTL_1 -> 0x10001010.U,
      s_waitData  -> 0x10001010.U,

      s_readRx_0  -> 0x10001000.U,
      s_readRx_1  -> 0x10001000.U,
      s_waitRx    -> 0x10001000.U,
    ))

    // mspi.io.in.pwrite := in.pwrite
    mspi.io.in.pwrite := MuxLookup(state, in.pwrite)(Seq (
      s_idle      -> in.pwrite,
      
      s_setTx_0   -> true.B,
      s_setTx_1   -> true.B,
      s_waitTx    -> true.B,

      s_setDiv_0  -> true.B,
      s_setDiv_1  -> true.B,
      s_waitDiv   -> true.B,

      s_setSS_0   -> true.B,
      s_setSS_1   -> true.B,
      s_waitSS    -> true.B,

      s_setCTL_0  -> true.B,
      s_setCTL_1  -> true.B,
      s_waitCTL   -> true.B,

      s_readCTL_0 -> false.B,
      s_readCTL_1 -> false.B,
      s_waitData  -> false.B,

      s_readRx_0  -> false.B,
      s_readRx_1  -> false.B,
      s_waitRx    -> false.B,
    ))

    // mspi.io.in.pwdata := in.pwdata
    mspi.io.in.pwdata := MuxLookup(state, in.pwdata)(Seq (
      s_idle      -> in.pwdata,

      s_setTx_0   -> (0x03000000.U | (in.paddr & 0x007fffff.U)),
      s_setTx_1   -> (0x03000000.U | (in.paddr & 0x007fffff.U)),
      s_waitTx    -> (0x03000000.U | (in.paddr & 0x007fffff.U)),
      // s_setTx_0   -> 0x03000000.U,
      // s_setTx_1   -> 0x03000000.U,
      // s_waitTx    -> 0x03000000.U,

      s_setDiv_0  -> 0x00000002.U,
      s_setDiv_1  -> 0x00000002.U,
      s_waitDiv   -> 0x00000002.U,

      s_setSS_0   -> 0x00000001.U,
      s_setSS_1   -> 0x00000001.U,
      s_waitSS    -> 0x00000001.U,

      s_setCTL_0  -> 0x00002140.U,
      s_setCTL_1  -> 0x00002140.U,
      s_waitCTL   -> 0x00002140.U,
      // s_setCTL_0  -> 256.U,
      // s_setCTL_1  -> 256.U,
      // s_waitCTL   -> 256.U,

      s_readCTL_0 -> 0.U,
      s_readCTL_1 -> 0.U,
      s_waitData  -> 0.U,

      s_readRx_0  -> 0.U,
      s_readRx_1  -> 0.U,
      s_waitRx    -> 0.U,
    ))

    // mspi.io.in.penable := in.penable
    mspi.io.in.penable := MuxLookup(state, in.penable)(Seq (
      s_idle      -> in.penable,
      
      s_setTx_0   -> false.B,
      s_setTx_1   -> true.B,
      s_waitTx    -> true.B,
      
      s_setDiv_0  -> false.B,
      s_setDiv_1  -> true.B,
      s_waitDiv   -> true.B,

      s_setSS_0   -> false.B,
      s_setSS_1   -> true.B,
      s_waitDiv   -> true.B,

      s_setCTL_0  -> false.B,
      s_setCTL_1  -> true.B,
      s_waitCTL   -> true.B,

      s_readCTL_0 -> false.B,
      s_readCTL_1 -> true.B,
      s_waitData  -> true.B,

      s_readRx_0  -> false.B,
      s_readRx_1  -> true.B,
      s_waitRx    -> true.B,
    )) && !(state === s_idle && read_flash)

    mspi.io.in.pstrb := Mux(state === s_idle, in.pstrb, 0xf.U)
    mspi.io.in.psel := Mux(state === s_idle, in.psel, true.B) && !(state === s_idle && read_flash)
    mspi.io.in.pprot := in.pprot
    in.pslverr := mspi.io.in.pslverr
    in.prdata := mspi.io.in.prdata

    in.pready := MuxCase(false.B, Seq(
      (state === s_idle && !read_flash) -> mspi.io.in.pready,
      (state === s_waitRx) -> mspi.io.in.pready
    ))
  }
}
