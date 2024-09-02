package cpu.ifu

import chisel3._
import chisel3.util._
import bus.AXI4IO
import cpu.ICachePerfCounter
import coursier.cache.loggers.RefreshInfo

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

    val cache = RegInit(VecInit(Seq.fill(S)(VecInit(Seq.fill(E)(0.U((B + t + 1).W))))))
    val group = cache(groupIndex)

    val tag = io.io.raddr(31, s + b)
    val groupIndex = Wire(UInt(s.W))
    if (s == 0) {
        groupIndex := 0.U(0.W)
    } else {
        groupIndex := io.io.raddr(s + b - 1, b)
    }
    val offset = io.io.raddr(b-1, 2)
    val mem_raddr = io.io.raddr(31, b)
    
    val lineHits = Wire(Vec(E, Bool()))
    for (i <- 0 to E-1) {
        lineHits(i) := group(i)(B + t - 1, B) === tag && group(i)(B + t) 
    }
    val isHit = lineHits.asUInt.orR
    val hitLineIndex = PriorityEncoder(lineHits)
    val hitEntry = group(hitLineIndex)

    val s_idle :: s_wait_mem_0 :: s_wait_mem_1 :: s_wait_mem_2 :: s_wait_mem_3 :: Nil = Enum(5)
    val state = RegInit(s_idle)
    state := MuxLookup(state, s_idle)(Seq (
        s_idle       -> Mux(io.io.ready, Mux(isHit, s_idle, Mux(io.mem.arready, s_wait_mem_0, s_idle)), s_idle),
        s_wait_mem_0 -> Mux(io.mem.rvalid, s_wait_mem_1, s_wait_mem_0),
        s_wait_mem_1 -> Mux(io.mem.rvalid, s_wait_mem_2, s_wait_mem_1),
        s_wait_mem_2 -> Mux(io.mem.rvalid, s_wait_mem_3, s_wait_mem_2),
        s_wait_mem_3 -> Mux(io.mem.rvalid, s_idle, s_wait_mem_3)
    ))
    val rdata0 = RegInit(0.U(32.W))
    val rdata1 = RegInit(0.U(32.W))
    val rdata2 = RegInit(0.U(32.W))
    rdata0 := Mux(io.mem.rvalid && state === s_wait_mem_0, io.mem.rdata, rdata0)    
    rdata1 := Mux(io.mem.rvalid && state === s_wait_mem_1, io.mem.rdata, rdata1)    
    rdata2 := Mux(io.mem.rvalid && state === s_wait_mem_2, io.mem.rdata, rdata2)    

    val ready = (state === s_idle) && io.io.ready
    val hitValid = ready && isHit
    val memValid = (state === s_wait_mem_3) && io.mem.rvalid
    val valid = hitValid || memValid
    
    val counter = RegInit(VecInit(Seq.fill(S)(0.U(e.W))))
    val groupCounter = counter(groupIndex)
    counter(groupIndex) := Mux(memValid, groupCounter+1.U, groupCounter)
    for (i <- 0 to E-1) {
        group(i) := Mux(
            memValid && groupCounter === i.U, 
            Cat(true.B, tag, io.mem.rdata, rdata2, rdata1, rdata0), 
            group(i)
        )
    }

    val memRData = MuxLookup(offset, 0.U)(Seq (
        0.U -> rdata0,
        1.U -> rdata1,
        2.U -> rdata2,
        3.U -> io.mem.rdata
    ))
    val hitData = MuxLookup(offset, 0.U)(Seq (
        0.U -> hitEntry(127, 96),
        1.U -> hitEntry( 95, 64),
        2.U -> hitEntry( 63, 32),
        3.U -> hitEntry( 31,  0)
    ))

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
