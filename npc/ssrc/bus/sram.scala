package bus

import chisel3._
import chisel3.util._

class SRAMBase extends Module {
    val io = IO(new Bundle {
        val waddr   = Input (UInt(32.W))
        val wdata   = Input (UInt(32.W))
        val wmask   = Input (UInt(8.W))
        val wen     = Input (Bool())
        val raddr   = Input (UInt(32.W))
        val rdata   = Output(UInt(32.W))
        val rsize   = Input (UInt(8.W))
        val ren     = Input (Bool())

        val valid   = Output(Bool())
        val ready   = Output(Bool())
    })
    val ready = RegInit(true.B)
    io.ready := ready

    val mem = Module(new Memory)
    mem.io.waddr := io.waddr
    mem.io.wdata := io.wdata
    mem.io.wmask := io.wmask
    mem.io.raddr := io.raddr
    io.rdata := mem.io.rdata
    mem.io.wen := io.wen & ready
    mem.io.ren := io.ren & ready
    mem.io.rsize := io.rsize
    mem.io.reset := reset

    val counter = RegInit(0.U(8.W))
    val valid = RegInit(0.B)
    when (io.ren || io.wen) {
        when (ready) {
            ready := false.B
            valid := false.B
            counter := 0.U
        }
    }
    when (!ready) {
        counter := counter + 1.U
        when (counter === 1.U) {
            valid := true.B
            ready := true.B
        }
    }
    io.valid := valid
}

class SRAM extends Module {
    val io = IO(new Bundle {
        val waddr   = Input (UInt(32.W))
        val wdata   = Input (UInt(32.W))
        val wmask   = Input (UInt(8.W))
        val wen     = Input (Bool())
        val raddr   = Input (UInt(32.W))
        val rdata   = Output(UInt(32.W))
        val ren     = Input (Bool())
        val valid   = Output(Bool())
    })
    val sram = Module(new SRAMBase)
    sram.io.waddr := io.waddr
    sram.io.wdata := io.wdata
    sram.io.wmask := io.wmask
    sram.io.wen   := io.wen
    sram.io.raddr := io.raddr
    sram.io.ren   := io.ren & !(reset.asBool)
    io.rdata := sram.io.rdata
    io.valid := sram.io.valid
}

class AXI4SRAM extends Module {
    // val io = IO(new AXILiteIO)
    val io = IO(Flipped(new AXI4IO))
    val sram = Module(new SRAMBase)

    // val s_wait_arvalid :: s_wait_rready :: Nil = Enum(2)
    // val r_state = RegInit(s_wait_arvalid)
    // val rdata_ready = sram.io.valid && io.r.ready
    // r_state := MuxLookup(r_state, s_wait_arvalid) (Seq (
    //     s_wait_arvalid -> Mux(io.ar.valid && sram.io.ready, s_wait_rready, s_wait_arvalid),
    //     s_wait_rready -> Mux(rdata_ready, s_wait_arvalid, s_wait_rready)
    // ))
    // io.ar.ready := r_state === s_wait_arvalid && sram.io.ready
    // io.r.data := sram.io.rdata
    // io.r.resp := 0.U
    // io.r.valid := sram.io.valid && r_state === s_wait_rready
    // sram.io.raddr := io.ar.addr
    // sram.io.ren := r_state === s_wait_rready && sram.io.ready

    // val s_wait_w_valid :: s_wait_sram_valid :: Nil = Enum(2)
    // val w_state = RegInit(s_wait_w_valid)
    // w_state := MuxLookup(w_state, s_wait_w_valid) (Seq (
    //     s_wait_w_valid -> Mux(io.w.valid && io.aw.valid && sram.io.ready, s_wait_sram_valid, s_wait_w_valid),
    //     s_wait_sram_valid -> Mux(sram.io.valid && io.b.ready, s_wait_w_valid, s_wait_sram_valid),
    // ))

    // sram.io.waddr := io.aw.addr
    // sram.io.wdata := io. w.data
    // sram.io.wmask := io. w.strb
    // sram.io.wen   := w_state === s_wait_sram_valid
    // io.aw.ready := w_state === s_wait_w_valid && sram.io.ready
    // io. w.ready := w_state === s_wait_w_valid && sram.io.ready
    
    // io. b.resp  := 0.U
    // io. b.valid := w_state === s_wait_sram_valid && sram.io.valid

    val s_wait_arvalid :: s_wait_rready :: Nil = Enum(2)
    val r_state = RegInit(s_wait_arvalid)
    val rdata_ready = sram.io.valid && io.rready
    r_state := MuxLookup(r_state, s_wait_arvalid) (Seq (
        s_wait_arvalid -> Mux(io.arvalid && sram.io.ready, s_wait_rready, s_wait_arvalid),
        s_wait_rready -> Mux(rdata_ready, s_wait_arvalid, s_wait_rready)
    ))
    io.arready := r_state === s_wait_arvalid && sram.io.ready
    io.rdata := sram.io.rdata
    io.rresp := 0.U
    io.rvalid := sram.io.valid && r_state === s_wait_rready
    sram.io.raddr := io.araddr
    sram.io.rsize := io.arsize
    sram.io.ren := r_state === s_wait_rready && sram.io.ready

    val s_wait_w_valid :: s_wait_sram_valid :: Nil = Enum(2)
    val w_state = RegInit(s_wait_w_valid)
    w_state := MuxLookup(w_state, s_wait_w_valid) (Seq (
        s_wait_w_valid -> Mux(io.wvalid && io.awvalid && sram.io.ready, s_wait_sram_valid, s_wait_w_valid),
        s_wait_sram_valid -> Mux(sram.io.valid && io.bready, s_wait_w_valid, s_wait_sram_valid),
    ))

    sram.io.waddr := io.awaddr
    sram.io.wdata := io. wdata
    sram.io.wmask := io. wstrb
    sram.io.wen   := w_state === s_wait_sram_valid
    io.awready := w_state === s_wait_w_valid && sram.io.ready
    io. wready := w_state === s_wait_w_valid && sram.io.ready
    
    io. bresp  := 0.U
    io. bvalid := w_state === s_wait_sram_valid && sram.io.valid

    // io.dbg := w_state
    
    // Not used
    io.bid := 0.U
    io.rlast := true.B
    io.rid := 0.U
}
