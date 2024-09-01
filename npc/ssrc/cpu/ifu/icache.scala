package cpu.ifu

import chisel3._
import chisel3.util._
import bus.AXI4IO
import cpu.ICachePerfCounter

class ICacheIO extends Bundle {
    val raddr = Input (UInt(32.W))
    val rdata = Output(UInt(32.W))
    val ready = Input (Bool())
    val valid = Output(Bool())
}

class ICache (e: Int, s: Int) extends Module {
    val io = IO(new Bundle {
        val io = new ICacheIO
        val mem = new AXI4IO()
        val perf = new ICachePerfCounter()
    })
    val b = 4
    val S = 1 << s
    val B = (1 << b) << 3
    val t = 32 - s - b
    val E = 1 << e

    val groupIndex = Wire(UInt(s.W))
    if (s == 0) {
        groupIndex := 0.U(0.W)
    } else {
        groupIndex := io.io.raddr(s + b - 1, b)
    }
    
    val tag = io.io.raddr(31, s + b)

    val cache = RegInit(VecInit(Seq.fill(S)(VecInit(Seq.fill(E)(0.U((B + t + 1).W))))))
    val group = cache(groupIndex)
    
    val lineHits = Wire(Vec(E, Bool()))
    for (i <- 0 to E-1) {
        lineHits(i) := group(i)(B + t - 1, B) === tag && group(i)(B + t) 
    }
    val isHit = lineHits.asUInt.orR
    val hitLineIndex = PriorityEncoder(lineHits)

    val s_idle :: s_wait_mem :: Nil = Enum(2)
    val state = RegInit(s_idle)
    state := MuxLookup(state, s_idle)(Seq (
        s_idle      -> Mux(io.io.ready, Mux(isHit, s_idle, Mux(io.mem.arready, s_wait_mem, s_idle)), s_idle),
        s_wait_mem  -> Mux(io.mem.rvalid, s_idle, s_wait_mem),
    ))

    val ready = (state === s_idle) && io.io.ready
    val hitValid = ready && isHit
    val memValid = (state === s_wait_mem) && io.mem.rvalid
    val valid = hitValid || memValid
    
    val counter = RegInit(VecInit(Seq.fill(S)(0.U(e.W))))
    val groupCounter = counter(groupIndex)
    counter(groupIndex) := Mux(memValid, groupCounter+1.U, groupCounter)
    for (i <- 0 to E-1) {
        group(i) := Mux(memValid && groupCounter === i.U, Cat(true.B, tag, io.mem.rdata), group(i))
    }

    io.io.valid := valid
    io.io.rdata := Mux(memValid, io.mem.rdata, group(hitLineIndex))

    io.mem.araddr := io.io.raddr
    io.mem.arvalid := ready && !isHit
    
    io.mem.rready := true.B
    io.mem.arlen  := 3.U // BURST 4
    io.mem.arsize := 2.U // 4 bytes per burst
    io.mem.arburst := 1.U // INCR

    io.perf.valid := memValid
    io.perf.isHit := hitValid
    io.perf.start := ready

    // Unused
    io.mem.bready  := false.B
    io.mem.wdata   := 0.U
    io.mem.wstrb   := 0.U
    io.mem.wvalid  := 0.U
    io.mem.awaddr  := 0.U
    io.mem.awvalid := 0.U
    io.mem.awid    := 0.U
    io.mem.awlen   := 0.U
    io.mem.awsize  := 0.U
    io.mem.awburst := 0.U
    io.mem.wlast   := false.B
    io.mem.arid    := 0.U
}   
