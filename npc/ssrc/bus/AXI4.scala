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

class AXI4Arbiter extends Module {
    val io = IO(new Bundle {
        // val io1 = Flipped(new AXI4IO)
        // val io2 = Flipped(new AXI4IO)
        // val sel = new AXI4IO
        val icache  = Flipped(new AXI4IO)
        val lsu     = Flipped(new AXI4IO)
        val sel     = new AXI4IO
    })
    // val select = RegInit(false.B)
    // val io1_valid = io.io1.arvalid || (io.io1.awvalid && io.io1.wvalid)
    // val io2_valid = io.io2.arvalid || (io.io2.awvalid && io.io2.wvalid)
    // val arbite_finished = (io.sel.rlast && io.sel.rready) || (io.sel.bvalid && io.sel.bready)
    // val request_arbite = io1_valid || io2_valid
     
    // val s_wait_in :: s_arbite :: Nil = Enum(2)
    // val state = RegInit(s_wait_in)
    // state := MuxLookup(state, s_wait_in)(Seq(
    //     s_wait_in -> Mux(request_arbite, s_arbite, s_wait_in),
    //     s_arbite  -> Mux(arbite_finished, s_wait_in, s_arbite)
    // ))
    // select := Mux(state === s_wait_in, Mux(io1_valid, 0.U, 1.U), select)

    // val arbiting = state === s_arbite
    // val select_io1 = arbiting && !select
    // io.io1.awready := Mux(select_io1, io.sel.awready, 0.U)
    // io.io1.wready  := Mux(select_io1, io.sel.wready , 0.U)
    // io.io1.bvalid  := Mux(select_io1, io.sel.bvalid , 0.U)
    // io.io1.bresp   := Mux(select_io1, io.sel.bresp  , 0.U)
    // io.io1.bid     := Mux(select_io1, io.sel.bid    , 0.U)
    // io.io1.arready := Mux(select_io1, io.sel.arready, 0.U)
    // io.io1.rvalid  := Mux(select_io1, io.sel.rvalid , 0.U)
    // io.io1.rresp   := Mux(select_io1, io.sel.rresp  , 0.U)
    // io.io1.rdata   := Mux(select_io1, io.sel.rdata  , 0.U)
    // io.io1.rlast   := Mux(select_io1, io.sel.rlast  , 0.U)
    // io.io1.rid     := Mux(select_io1, io.sel.rid    , 0.U)

    // val select_io2 = arbiting && select
    // io.io2.awready := Mux(select_io2, io.sel.awready, 0.U)
    // io.io2.wready  := Mux(select_io2, io.sel.wready , 0.U)
    // io.io2.bvalid  := Mux(select_io2, io.sel.bvalid , 0.U)
    // io.io2.bresp   := Mux(select_io2, io.sel.bresp  , 0.U)
    // io.io2.bid     := Mux(select_io2, io.sel.bid    , 0.U)
    // io.io2.arready := Mux(select_io2, io.sel.arready, 0.U)
    // io.io2.rvalid  := Mux(select_io2, io.sel.rvalid , 0.U)
    // io.io2.rresp   := Mux(select_io2, io.sel.rresp  , 0.U)
    // io.io2.rdata   := Mux(select_io2, io.sel.rdata  , 0.U)
    // io.io2.rlast   := Mux(select_io2, io.sel.rlast  , 0.U)
    // io.io2.rid     := Mux(select_io2, io.sel.rid    , 0.U)

    // io.sel.awvalid := Mux(arbiting, Mux(select_io1, io.io1.awvalid, io.io2.awvalid), 0.U)
    // io.sel.awaddr  := Mux(arbiting, Mux(select_io1, io.io1.awaddr , io.io2.awaddr ), 0.U)
    // io.sel.awid    := Mux(arbiting, Mux(select_io1, io.io1.awid   , io.io2.awid   ), 0.U)
    // io.sel.awlen   := Mux(arbiting, Mux(select_io1, io.io1.awlen  , io.io2.awlen  ), 0.U)
    // io.sel.awsize  := Mux(arbiting, Mux(select_io1, io.io1.awsize , io.io2.awsize ), 0.U)
    // io.sel.awburst := Mux(arbiting, Mux(select_io1, io.io1.awburst, io.io2.awburst), 0.U)
    // io.sel.wvalid  := Mux(arbiting, Mux(select_io1, io.io1.wvalid , io.io2.wvalid ), 0.U)
    // io.sel.wdata   := Mux(arbiting, Mux(select_io1, io.io1.wdata  , io.io2.wdata  ), 0.U)
    // io.sel.wstrb   := Mux(arbiting, Mux(select_io1, io.io1.wstrb  , io.io2.wstrb  ), 0.U)
    // io.sel.wlast   := Mux(arbiting, Mux(select_io1, io.io1.wlast  , io.io2.wlast  ), 0.U)
    // io.sel.bready  := Mux(arbiting, Mux(select_io1, io.io1.bready , io.io2.bready ), 0.U)
    // io.sel.arvalid := Mux(arbiting, Mux(select_io1, io.io1.arvalid, io.io2.arvalid), 0.U)
    // io.sel.araddr  := Mux(arbiting, Mux(select_io1, io.io1.araddr , io.io2.araddr ), 0.U)
    // io.sel.arid    := Mux(arbiting, Mux(select_io1, io.io1.arid   , io.io2.arid   ), 0.U)
    // io.sel.arlen   := Mux(arbiting, Mux(select_io1, io.io1.arlen  , io.io2.arlen  ), 0.U)
    // io.sel.arsize  := Mux(arbiting, Mux(select_io1, io.io1.arsize , io.io2.arsize ), 0.U)
    // io.sel.arburst := Mux(arbiting, Mux(select_io1, io.io1.arsize , io.io2.arsize ), 0.U)
    // io.sel.rready  := Mux(arbiting, Mux(select_io1, io.io1.rready , io.io2.rready ), 0.U)
    // io.sel.awvalid := Mux(arbiting, Mux(select, io.io2.awvalid, io.io1.awvalid), 0.U)
    // io.sel.awaddr  := Mux(arbiting, Mux(select, io.io2.awaddr , io.io1.awaddr ), 0.U)
    // io.sel.awid    := Mux(arbiting, Mux(select, io.io2.awid   , io.io1.awid   ), 0.U)
    // io.sel.awlen   := Mux(arbiting, Mux(select, io.io2.awlen  , io.io1.awlen  ), 0.U)
    // io.sel.awsize  := Mux(arbiting, Mux(select, io.io2.awsize , io.io1.awsize ), 0.U)
    // io.sel.awburst := Mux(arbiting, Mux(select, io.io2.awburst, io.io1.awburst), 0.U)
    // io.sel.wvalid  := Mux(arbiting, Mux(select, io.io2.wvalid , io.io1.wvalid ), 0.U)
    // io.sel.wdata   := Mux(arbiting, Mux(select, io.io2.wdata  , io.io1.wdata  ), 0.U)
    // io.sel.wstrb   := Mux(arbiting, Mux(select, io.io2.wstrb  , io.io1.wstrb  ), 0.U)
    // io.sel.wlast   := Mux(arbiting, Mux(select, io.io2.wlast  , io.io1.wlast  ), 0.U)
    // io.sel.bready  := Mux(arbiting, Mux(select, io.io2.bready , io.io1.bready ), 0.U)
    // io.sel.arvalid := Mux(arbiting, Mux(select, io.io2.arvalid, io.io1.arvalid), 0.U)
    // io.sel.araddr  := Mux(arbiting, Mux(select, io.io2.araddr , io.io1.araddr ), 0.U)
    // io.sel.arid    := Mux(arbiting, Mux(select, io.io2.arid   , io.io1.arid   ), 0.U)
    // io.sel.arlen   := Mux(arbiting, Mux(select, io.io2.arlen  , io.io1.arlen  ), 0.U)
    // io.sel.arsize  := Mux(arbiting, Mux(select, io.io2.arsize , io.io1.arsize ), 0.U)
    // io.sel.arburst := Mux(arbiting, Mux(select, io.io2.arsize , io.io1.arsize ), 0.U)
    // io.sel.rready  := Mux(arbiting, Mux(select, io.io2.rready , io.io1.rready ), 0.U)
    val icacheReqArb = io.icache.arvalid
    val lsuReqArb = io.lsu.arvalid
    val requestArbite = icacheReqArb || lsuReqArb
    val arbiteEnd = io.sel.rlast && io.sel.rready
    
    val s_idle :: s_arbite :: Nil = Enum(2)
    val state = RegInit(s_idle)
    state := MuxLookup(state, s_idle)(Seq(
        s_idle   -> Mux(requestArbite,  s_arbite, s_idle),
        s_arbite -> Mux(arbiteEnd,      s_idle,   s_arbite),
    ))

    val select = RegEnable(!icacheReqArb, state === s_idle)
    val arbiting = state === s_arbite

    io.sel.arvalid := Mux(arbiting, Mux(select, io.lsu.arvalid, io.icache.arvalid), false.B)
    io.sel.araddr  := Mux(arbiting, Mux(select, io.lsu.araddr,  io.icache.araddr),  12345678.U)
    io.sel.arlen   := Mux(arbiting, Mux(select, io.lsu.arlen,   io.icache.arlen),   DontCare)
    io.sel.arsize  := Mux(arbiting, Mux(select, io.lsu.arsize,  io.icache.arsize),  DontCare)
    io.sel.arburst := Mux(arbiting, Mux(select, io.lsu.arburst, io.icache.arburst), DontCare)
    io.sel.rready  := Mux(arbiting, Mux(select, io.lsu.rready,  io.icache.rready),  false.B)
    io.sel.arid    := DontCare

    io.icache.arready := Mux(arbiting && !select, io.sel.arready, false.B)
    io.icache.rvalid  := Mux(arbiting && !select, io.sel.rvalid,  false.B)
    io.icache.rresp := io.sel.rresp
    io.icache.rdata := io.sel.rdata
    io.icache.rlast := io.sel.rlast
    io.icache.rid   := DontCare

    io.lsu.arready := Mux(arbiting && select, io.sel.arready, false.B)
    io.lsu.rvalid  := Mux(arbiting && select, io.sel.rvalid,  false.B)
    io.lsu.rresp := io.sel.rresp
    io.lsu.rdata := io.sel.rdata
    io.lsu.rlast := io.sel.rlast
    io.lsu.rid   := DontCare

    io.icache.awready := DontCare
    io.icache.wready  := DontCare
    io.icache.bvalid  := DontCare
    io.icache.bresp   := DontCare
    io.icache.bid     := DontCare

    io.lsu.awready := io.sel.awready
    io.lsu.wready  := io.sel.wready
    io.lsu.bvalid  := io.sel.bvalid
    io.lsu.bresp   := io.sel.bresp
    io.lsu.bid     := DontCare
    
    io.sel.awvalid := io.lsu.awvalid
    io.sel.awaddr  := io.lsu.awaddr
    io.sel.awid    := DontCare
    io.sel.awlen   := io.lsu.awlen
    io.sel.awsize  := io.lsu.awsize
    io.sel.awburst := io.lsu.awburst
    io.sel.wvalid  := io.lsu.wvalid
    io.sel.wdata   := io.lsu.wdata
    io.sel.wstrb   := io.lsu.wstrb
    io.sel.wlast   := io.lsu.wlast
    io.sel.bready  := io.lsu.bready

    // when(state === s_idle && requestArbite) {
    //     printf("Start arbite %d 0x%x\n", io.lsu.arvalid, io.sel.araddr)
    // }
    // when(state === s_arbite) {
    //     printf("arbite %d 0x%x\n", select, io.sel.araddr)
    // }
    // when(state === s_arbite && arbiteEnd) {
    //     printf("End arbite %d\n", select)
    // }
    // when(io.sel.rvalid) {
    //     printf("RVALID\n")
    // }
    // when(state === s_idle && icacheReqArb) {
    //     printf("icache\n")
    // }
    // when(state === s_idle && lsuReqArb) {
    //     printf("lsu\n")
    // }
}
