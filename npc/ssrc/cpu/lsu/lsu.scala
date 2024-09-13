package cpu.lsu

import chisel3._
import chisel3.util._

import cpu.EXUMessage
import cpu.LSUMessage

import bus.AXI4IO
import cpu.LSUPerfCounter
import cpu.reg.GPRWSel

object MemType{
    val B  = 0.U
    val H  = 1.U
    val W  = 2.U
    val BU = 4.U
    val HU = 5.U
}

class LSU extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new EXUMessage))
        val out = Decoupled(new LSUMessage)

        val mem = new AXI4IO
        
        val perf = new LSUPerfCounter
    })
    val memWen = io.in.bits.mem_wen && io.in.valid
    val memEnable = (io.in.bits.mem_ren || io.in.bits.mem_wen) && io.in.valid
    val memValid = Mux(io.in.bits.mem_ren, io.mem.rvalid,  io.mem.bvalid)
    val memReady = Mux(io.in.bits.mem_ren, io.mem.arready, io.mem.awready && io.mem.wready)
    
    val s_idle :: s_wait_mem_ready :: s_wait_mem_valid :: s_valid :: Nil = Enum(4)
    val state = RegInit(s_idle)
    state := MuxLookup(state, s_idle)(Seq (
        s_idle -> Mux(memEnable, s_wait_mem_ready, s_idle),
        s_wait_mem_ready -> Mux(memReady, s_wait_mem_valid, s_wait_mem_ready),
        s_wait_mem_valid -> Mux(memValid, s_idle,           s_wait_mem_valid),
    ))

    // COMMON
    val addr = io.in.bits.exu_result
    val offset = addr(1,0)
    val memType = io.in.bits.func3
    val size = Cat(0.B, memType(1, 0))

    // WRITE
    // WMASK
    val wmask_b = MuxLookup(offset, 0.U(4.W))(Seq (
        0.U -> 0b0001.U(4.W),
        1.U -> 0b0010.U(4.W),
        2.U -> 0b0100.U(4.W),
        3.U -> 0b1000.U(4.W)
    ))
    val wmask_h = MuxLookup(offset, 0.U(4.W))(Seq (
        0.U -> 0b0011.U(4.W),
        2.U -> 0b1100.U(4.W)
    ))
    val wmask_w = 0b1111.U(4.W)
    val wmask = Mux(memType(1), wmask_w, Mux(memType(0), wmask_h, wmask_b))

    // WDATA
    val rs = io.in.bits.mem_wdata
    val wdata_b = Fill(4, rs( 7, 0))
    val wdata_h = Fill(2, rs(15, 0))
    val wdata_w = rs
    val wdata = Mux(memType(1), wdata_w, Mux(memType(0), wdata_h, wdata_b))

    val wValid = memWen && state === s_wait_mem_ready
    io.mem.awaddr  := addr
    io.mem.awvalid := wValid
    io.mem.wdata   := wdata
    io.mem.wvalid  := wValid
    io.mem.wstrb   := wmask
    io.mem.awsize  := size
    io.mem.bready  := memWen && state === s_wait_mem_valid

    // READ
    val memRen = io.in.valid && io.in.bits.mem_ren
    io.mem.araddr  := addr
    io.mem.arvalid := memRen && state === s_wait_mem_ready
    io.mem.arsize  := size
    io.mem.rready  := memRen && state === s_wait_mem_valid && io.out.ready

    val origin_rdata_0 = io.mem.rdata( 7,  0)
    val origin_rdata_1 = io.mem.rdata(15,  8)
    val origin_rdata_2 = io.mem.rdata(23, 16)
    val origin_rdata_3 = io.mem.rdata(31, 24)
    val mem_rdata_sign = MuxLookup(offset, 0.U)(Seq(
        0.U -> Mux(Mux(memType(0), io.mem.rdata(15), io.mem.rdata( 7)), 255.U(8.W), 0.U(8.W)),
        1.U -> Mux(io.mem.rdata(15), 255.U(8.W), 0.U(8.W)),
        2.U -> Mux(Mux(memType(0), io.mem.rdata(31), io.mem.rdata(23)), 255.U(8.W), 0.U(8.W)),
        3.U -> Mux(io.mem.rdata(31), 255.U(8.W), 0.U(8.W)),
    ))
    val mem_rdata_0 = MuxLookup(offset, 0.U)(Seq(
        0.U -> origin_rdata_0,
        1.U -> origin_rdata_1,
        2.U -> origin_rdata_2,
        3.U -> origin_rdata_3
    ))
    val mem_rdata_1_h =MuxLookup(offset, 0.U)(Seq(
        0.U -> io.mem.rdata(15,  8),
        2.U -> io.mem.rdata(31, 24)
    ))
    val mem_rdata_1 = Mux(memType(1), origin_rdata_1, Mux(memType(0), mem_rdata_1_h, Mux(memType(2), 0.U(8.W), mem_rdata_sign)))
    val mem_rdata_2 = Mux(memType(1), origin_rdata_2, Mux(memType(2), 0.U(8.W), mem_rdata_sign))
    val mem_rdata_3 = Mux(memType(1), origin_rdata_3, Mux(memType(2), 0.U(8.W), mem_rdata_sign))
    
    // val mem_rdata = RegInit(0.U(32.W))
    val mem_rdata = Cat(mem_rdata_3, mem_rdata_2, mem_rdata_1, mem_rdata_0)
    val gpr_wdata = Mux(io.in.bits.gpr_ws(1), Mux(io.in.bits.gpr_ws(0), mem_rdata, io.in.bits.exu_result), io.in.bits.gpr_wdata)
    io.out.bits.gpr_wdata := gpr_wdata

    val done = (state === s_wait_mem_valid && memValid)
    val nothingToDo = state === s_idle && !(memRen || memWen)
    io.in.ready  := (nothingToDo || done) && io.out.ready
    io.out.valid := (nothingToDo || done) && io.in.valid
    
    // Unused
    io.mem.awid    := 0.U
    io.mem.awlen   := 0.U
    io.mem.awburst := 0.U
    io.mem.wlast   := true.B
    io.mem.arid    := 0.U
    io.mem.arlen   := 0.U
    io.mem.arburst := 0.U

    // Passthrough        
    io.out.bits.gpr_waddr  := io.in.bits.gpr_waddr
    io.out.bits.gpr_wen    := io.in.bits.gpr_wen

    io.out.bits.is_brk := io.in.bits.is_brk
    io.out.bits.is_ivd := io.in.bits.is_ivd

    // PERF
    io.perf.isWaiting := state === s_wait_mem_valid
    io.perf.addr := addr
    io.perf.wen := io.in.bits.mem_wen
    io.perf.ren := io.in.bits.mem_ren

    // DEBUG
    io.out.bits.dbg <> io.in.bits.dbg
}
