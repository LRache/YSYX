package bus

import chisel3._
import chisel3.util._

class AXI4IO extends Bundle {
    // AW
    val awready = Input (Bool())
    val awvalid = Output(Bool())
    val awaddr  = Output(UInt(32.W))
    val awid    = Output(UInt(4.W))
    val awlen   = Output(UInt(8.W))
    val awsize  = Output(UInt(3.W))
    val awburst = Output(UInt(2.W))

    // W
    val wready = Input (Bool())
    val wvalid = Output(Bool())
    val wdata  = Output(UInt(32.W))
    val wstrb  = Output(UInt(4.W))
    val wlast  = Output(Bool())

    // B
    val bready = Output(Bool())
    val bvalid = Input (Bool())
    val bresp  = Input (UInt(2.W))
    val bid    = Input (UInt(4.W))

    // AR
    val arready = Input (Bool())
    val arvalid = Output(Bool())
    val araddr  = Output(UInt(32.W))
    val arid    = Output(UInt(4.W))
    val arlen   = Output(UInt(8.W))
    val arsize  = Output(UInt(3.W))
    val arburst = Output(UInt(2.W))

    // R
    val rready = Output(Bool())
    val rvalid = Input (Bool())
    val rresp  = Input (UInt(2.W))
    val rdata  = Input (UInt(32.W))
    val rlast  = Input (Bool())
    val rid    = Input (UInt(4.W))
}

object AXI4ArbiterInline {
    def apply(
        icache: AXI4IO,
        lsu:    AXI4IO,
        sel:    AXI4IO
    ): Unit = {
        val icacheReqArb = icache.arvalid
        val lsuReqArb = lsu.arvalid
        val requestArbite = icacheReqArb || lsuReqArb
        val arbiteEnd = sel.rlast && sel.rready
        
        val s_idle :: s_arbite :: Nil = Enum(2)
        val state = RegInit(s_idle)
        state := MuxLookup(state, s_idle)(Seq(
            s_idle   -> Mux(requestArbite,  s_arbite, s_idle),
            s_arbite -> Mux(arbiteEnd,      s_idle,   s_arbite),
        ))

        val select = RegEnable(!icacheReqArb, state === s_idle)
        val arbiting = state === s_arbite

        sel.arvalid := Mux(arbiting, Mux(select, lsu.arvalid, icache.arvalid), false.B)
        sel.araddr  := Mux(arbiting, Mux(select, lsu.araddr,  icache.araddr),  DontCare)
        sel.arlen   := Mux(arbiting, Mux(select, lsu.arlen,   icache.arlen),   DontCare)
        sel.arsize  := Mux(arbiting, Mux(select, lsu.arsize,  icache.arsize),  DontCare)
        sel.arburst := Mux(arbiting, Mux(select, lsu.arburst, icache.arburst), DontCare)
        sel.rready  := Mux(arbiting, Mux(select, lsu.rready,  icache.rready),  false.B)
        sel.arid    := DontCare

        icache.arready := Mux(arbiting && !select, sel.arready, false.B)
        icache.rvalid  := Mux(arbiting && !select, sel.rvalid,  false.B)
        icache.rresp := sel.rresp
        icache.rdata := sel.rdata
        icache.rlast := sel.rlast
        icache.rid   := DontCare

        lsu.arready := Mux(arbiting && select, sel.arready, false.B)
        lsu.rvalid  := Mux(arbiting && select, sel.rvalid,  false.B)
        lsu.rresp := sel.rresp
        lsu.rdata := sel.rdata
        lsu.rlast := sel.rlast
        lsu.rid   := DontCare

        icache.awready := DontCare
        icache.wready  := DontCare
        icache.bvalid  := DontCare
        icache.bresp   := DontCare
        icache.bid     := DontCare

        lsu.awready := sel.awready
        lsu.wready  := sel.wready
        lsu.bvalid  := sel.bvalid
        lsu.bresp   := sel.bresp
        lsu.bid     := DontCare
        
        sel.awvalid := lsu.awvalid
        sel.awaddr  := lsu.awaddr
        sel.awid    := DontCare
        sel.awlen   := 0.U
        sel.awsize  := lsu.awsize
        sel.awburst := 0.U
        sel.wvalid  := lsu.wvalid
        sel.wdata   := lsu.wdata
        sel.wstrb   := lsu.wstrb
        sel.wlast   := true.B
        sel.bready  := lsu.bready
    }
}

class AXI4Arbiter extends Module {
    val io = IO(new Bundle {
        val icache  = Flipped(new AXI4IO)
        val lsu     = Flipped(new AXI4IO)
        val sel     = new AXI4IO
    })
    // val icacheReqArb = io.icache.arvalid
    // val lsuReqArb = io.lsu.arvalid
    // val requestArbite = icacheReqArb || lsuReqArb
    // val arbiteEnd = io.sel.rlast && io.sel.rready
    
    // val s_idle :: s_arbite :: Nil = Enum(2)
    // val state = RegInit(s_idle)
    // state := MuxLookup(state, s_idle)(Seq(
    //     s_idle   -> Mux(requestArbite,  s_arbite, s_idle),
    //     s_arbite -> Mux(arbiteEnd,      s_idle,   s_arbite),
    // ))

    // val select = RegEnable(!icacheReqArb, state === s_idle)
    // val arbiting = state === s_arbite

    // io.sel.arvalid := Mux(arbiting, Mux(select, io.lsu.arvalid, io.icache.arvalid), false.B)
    // io.sel.araddr  := Mux(arbiting, Mux(select, io.lsu.araddr,  io.icache.araddr),  DontCare)
    // io.sel.arlen   := Mux(arbiting, Mux(select, io.lsu.arlen,   io.icache.arlen),   DontCare)
    // io.sel.arsize  := Mux(arbiting, Mux(select, io.lsu.arsize,  io.icache.arsize),  DontCare)
    // io.sel.arburst := Mux(arbiting, Mux(select, io.lsu.arburst, io.icache.arburst), DontCare)
    // io.sel.rready  := Mux(arbiting, Mux(select, io.lsu.rready,  io.icache.rready),  false.B)
    // io.sel.arid    := DontCare

    // io.icache.arready := Mux(arbiting && !select, io.sel.arready, false.B)
    // io.icache.rvalid  := Mux(arbiting && !select, io.sel.rvalid,  false.B)
    // io.icache.rresp := io.sel.rresp
    // io.icache.rdata := io.sel.rdata
    // io.icache.rlast := io.sel.rlast
    // io.icache.rid   := DontCare

    // io.lsu.arready := Mux(arbiting && select, io.sel.arready, false.B)
    // io.lsu.rvalid  := Mux(arbiting && select, io.sel.rvalid,  false.B)
    // io.lsu.rresp := io.sel.rresp
    // io.lsu.rdata := io.sel.rdata
    // io.lsu.rlast := io.sel.rlast
    // io.lsu.rid   := DontCare

    // io.icache.awready := DontCare
    // io.icache.wready  := DontCare
    // io.icache.bvalid  := DontCare
    // io.icache.bresp   := DontCare
    // io.icache.bid     := DontCare

    // io.lsu.awready := io.sel.awready
    // io.lsu.wready  := io.sel.wready
    // io.lsu.bvalid  := io.sel.bvalid
    // io.lsu.bresp   := io.sel.bresp
    // io.lsu.bid     := DontCare
    
    // io.sel.awvalid := io.lsu.awvalid
    // io.sel.awaddr  := io.lsu.awaddr
    // io.sel.awid    := DontCare
    // io.sel.awlen   := 0.U
    // io.sel.awsize  := io.lsu.awsize
    // io.sel.awburst := 0.U
    // io.sel.wvalid  := io.lsu.wvalid
    // io.sel.wdata   := io.lsu.wdata
    // io.sel.wstrb   := io.lsu.wstrb
    // io.sel.wlast   := true.B
    // io.sel.bready  := io.lsu.bready
    AXI4ArbiterInline(
        icache = io.icache,
        lsu = io.lsu,
        sel = io.sel
    )
}
