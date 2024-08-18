package ysyx

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import freechips.rocketchip.regmapper.RRTest0Map.de

class SDRAMIO extends Bundle {
  val clk = Output(Bool())
  val cke = Output(Bool())
  val cs  = Output(Bool())
  val ras = Output(Bool())
  val cas = Output(Bool())
  val we  = Output(Bool())
  val a   = Output(UInt(13.W))
  val ba  = Output(UInt(2.W))
  val dqm = Output(UInt(2.W))
  val dq  = Analog(16.W)
}

class sdram_top_axi extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val in = Flipped(new AXI4Bundle(AXI4BundleParameters(addrBits = 32, dataBits = 32, idBits = 4)))
    val sdram = new SDRAMIO
  })
}

class sdram_top_apb extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    val sdram = new SDRAMIO
  })
}

class sdram extends BlackBox {
  val io = IO(Flipped(new SDRAMIO))
}

class SDRAMBase extends BlackBox {
  val io = IO(new Bundle {
    val clk   = Input(Clock())

    val bank  = Input(UInt(2.W))
    val row   = Input(UInt(13.W))
    val col   = Input(UInt(9.W))
    
    val wen   = Input(Bool())
    val wdata = Input(UInt(16.W))
    val wmask = Input(UInt(2.W))
    val ren   = Input(Bool())
    val rdata = Output(UInt(16.W))
  })
}

class sdramChisel extends RawModule {
  val io = IO(Flipped(new SDRAMIO))

  val sdram0 = Module(new SDRAMBase)

  val s_idle :: s_read_lantency :: s_read :: s_write :: Nil = Enum(4)

  withClockAndReset(io.clk.asClock, 0.B) {
    val state = Reg(UInt(3.W))
    val latency = Reg(UInt(2.W))
    val burst = Reg(UInt(3.W))
    val op = Reg(Bool())
    val outEn = Reg(Bool())
    val din = TriStateInBuf(io.dq, sdram0.io.rdata, outEn)

    outEn := state === s_read
    
    val bank  = Reg(UInt(2.W))
    val row   = Reg(Vec(4, UInt(13.W)))
    val col   = Reg(UInt(9.W))
    val wdata = Reg(UInt(16.W))
    val wmask = Reg(UInt(2.W))
    val counter = Reg(UInt(3.W))
    val delay   = Reg(UInt(3.W))
    val ren     = Reg(Bool())

    sdram0.io.clk    := io.clk.asClock
    sdram0.io.bank   := bank
    sdram0.io.row    := row(bank)
    sdram0.io.col    := col
    sdram0.io.wen    := state === s_write
    sdram0.io.wdata  := wdata
    sdram0.io.wmask  := wmask
    sdram0.io.ren    := ren

    val is_write    = !io.cs &&  io.ras && !io.cas && !io.we
    val is_read     = !io.cs &&  io.ras && !io.cas &&  io.we
    val is_set_mode = !io.cs && !io.ras && !io.cas && !io.we
    val is_act_row  = !io.cs && !io.ras &&  io.cas &&  io.we
    val is_burst_ter= !io.cs &&  io.ras &&  io.cas && !io.we
    val is_precharge= !io.cs && !io.ras &&  io.cas && !io.we

    def change_to_write(column: UInt) = {
        counter := 0.U
        bank    := io.ba
        col     := io.a(8,0) + column
        wdata   := din
        wmask   := io.dqm
        ren     := false.B
    }
    def change_to_read() {
        counter := 0.U
        bank    := io.ba
        col     := io.a(8,0)
        delay   := 0.U
        ren     := latency === 0.U
        state   := Mux(latency === 0.U, s_read, s_read_lantency)
    }

    def change_idle() = {
        when (is_act_row) {
          state := s_idle
          row(io.ba):= io.a
        } 
        .elsewhen(is_set_mode) {
            state := s_idle
            burst := MuxLookup(io.a(2,0), 1.U(3.W)) (Seq (
              0.U(3.W) -> 0.U,
              1.U(3.W) -> 1.U,
              2.U(3.W) -> 3.U,
              3.U(3.W) -> 7.U,
            ))
            latency := MuxLookup(io.a(6,4), 0.U(2.W)) (Seq (
              2.U(2.W) -> 0.U,
              3.U(3.W) -> 1.U
            ))
            ren := false.B
        } 
        .elsewhen(is_write) {
            state := s_write
            change_to_write(0.U)
        }
        .elsewhen(is_read) {
            change_to_read()
        }
        .elsewhen(is_burst_ter) {
            ren := false.B
        }
        .elsewhen(is_precharge) {
        }
        .otherwise {
            state := s_idle
            ren   := false.B
        }
    }

    when(state === s_write) {
      when (counter === burst) {
        change_idle()
      } .otherwise {
        state   := s_write
        counter := counter + 1.U
        col     := col + 1.U
        wdata   := din
        wmask   := io.dqm
      }
    }.elsewhen(state === s_read_lantency) {
      when (delay === latency) {
        state   := s_read
        counter := counter + 1.U
        col     := col + 1.U
        ren     := true.B
      } .otherwise {
        delay := delay + 1.U
      }
    } .elsewhen(state === s_read) {
      when (counter === burst) {
        change_idle()
      } .otherwise {
        state   := s_read
        counter := counter + 1.U
        col     := col + 1.U
      }
    } .otherwise {
      change_idle()
    }
  }
}

class AXI4SDRAM(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
  val beatBytes = 4
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    Seq(AXI4SlaveParameters(
        address       = address,
        executable    = true,
        supportsWrite = TransferSizes(1, beatBytes),
        supportsRead  = TransferSizes(1, beatBytes),
        interleavedId = Some(0))
    ),
    beatBytes  = beatBytes)))

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)
    val sdram_bundle = IO(new SDRAMIO)

    val msdram = Module(new sdram_top_axi)
    msdram.io.clock := clock
    msdram.io.reset := reset.asBool
    msdram.io.in <> in
    sdram_bundle <> msdram.io.sdram
  }
}

class APBSDRAM(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val sdram_bundle = IO(new SDRAMIO)

    val msdram = Module(new sdram_top_apb)
    msdram.io.clock := clock
    msdram.io.reset := reset.asBool
    msdram.io.in <> in
    sdram_bundle <> msdram.io.sdram
  }
}
