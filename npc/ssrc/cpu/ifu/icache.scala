package cpu.ifu

import scala.collection.mutable.ArrayBuffer

import chisel3._
import chisel3.util._

import bus.AXI4IO
import cpu.ICachePerfCounter

class ICacheIO extends Bundle {
    val raddr = Input (UInt(30.W))
    val rdata = Output(UInt(32.W))
    val ready = Input (Bool())
    val valid = Output(Bool())
}

class ICache (e: Int, s: Int) extends Module {
    val io = IO(new Bundle {
        val io    = new ICacheIO
        val mem   = new AXI4IO()
        val perf  = new ICachePerfCounter()
        val fence = Input(Bool())
    })
    val b = 4
    val S = 1 << s
    val B = (1 << b) << 3
    val t = 32 - s - b
    val E = 1 << e

    val tag = io.io.raddr(31 - 2, s + b - 2)
    val groupIndex = Wire(UInt(s.W))
    if (s == 0) {
        groupIndex := 0.U(0.W)
    } else {
        groupIndex := io.io.raddr(s + b - 1 - 2, b - 2)
    }
    val offset = io.io.raddr(b - 1 - 2, 2 - 2)
    val memRAddr = Cat(io.io.raddr(31 - 2, b - 2), 0.U(b.W))

    val cache     = Reg(Vec(S, Vec(E, Vec(4, UInt(32.W)))))
    // val cache     = RegInit(VecInit(Seq.fill(S)(VecInit(Seq.fill(E)(VecInit(Seq.fill(4)(0.U(32.W))))))))
    // val metaTag   = RegInit(VecInit(Seq.fill(S)(VecInit(Seq.fill(E)(0.U((t).W))))))
    val metaTag   = Reg(Vec(S, Vec(E, UInt(t.W))))
    val metaValid = RegInit(VecInit(Seq.fill(S)(VecInit(Seq.fill(E)(false.B)))))
    val hitGroup  = cache(groupIndex)
    
    val lineHits = Wire(Vec(E, Bool()))
    for (i <- 0 to E-1) {
        lineHits(i) := metaTag(groupIndex)(i) === tag && metaValid(groupIndex)(i)
    }
    val isHit = lineHits.asUInt.orR
    val hitLineIndex = PriorityEncoder(lineHits)
    val hitEntry = hitGroup(hitLineIndex)

    val s_idle :: s_wait_mem_0 :: s_wait_mem_1 :: s_wait_mem_2 :: s_wait_mem_3 :: s_mem_valid :: Nil = Enum(6)
    val state = RegInit(s_idle)
    state := MuxLookup(state, s_idle)(Seq (
        s_idle       -> Mux(io.io.ready, Mux(isHit, s_idle, Mux(io.mem.arready, s_wait_mem_0, s_idle)), s_idle),
        s_wait_mem_0 -> Mux(io.mem.rvalid, s_wait_mem_1, s_wait_mem_0),
        s_wait_mem_1 -> Mux(io.mem.rvalid, s_wait_mem_2, s_wait_mem_1),
        s_wait_mem_2 -> Mux(io.mem.rvalid, s_wait_mem_3, s_wait_mem_2),
        s_wait_mem_3 -> Mux(io.mem.rvalid, s_mem_valid,  s_wait_mem_3),
        s_mem_valid  -> s_idle,
    )) 

    val ready = (state === s_idle) && io.io.ready
    val hitValid = ready && isHit
    val memValid = (state === s_wait_mem_3) && io.mem.rvalid
    
    val counter = RegInit(VecInit(Seq.fill(S)(0.U(e.W))))
    val groupCounter = counter(groupIndex)
    counter(groupIndex) := Mux(memValid, groupCounter+1.U, groupCounter)
    
    for (i <- 0 until S) {
        for (j <- 0 until E) {
            val select = groupIndex === i.U && groupCounter === j.U
            cache(i)(j)(0) := Mux(io.mem.rvalid && state === s_wait_mem_0 && select, io.mem.rdata, cache(i)(j)(0))
            cache(i)(j)(1) := Mux(io.mem.rvalid && state === s_wait_mem_1 && select, io.mem.rdata, cache(i)(j)(1))
            cache(i)(j)(2) := Mux(io.mem.rvalid && state === s_wait_mem_2 && select, io.mem.rdata, cache(i)(j)(2))
            cache(i)(j)(3) := Mux(memValid && select, io.mem.rdata, cache(i)(j)(3))
            metaTag(i)(j)   := Mux(memValid && select, tag, metaTag(i)(j))
            metaValid(i)(j) := Mux(memValid && select, true.B, Mux(io.fence, false.B, metaValid(i)(j)))
        }
    }

    val hitDataLookupArray : ArrayBuffer[(UInt, UInt)] = ArrayBuffer()
    for (i <- 0 until S) {
        val entryLookupArray : ArrayBuffer[(UInt, UInt)] = ArrayBuffer()
        for (j <- 0 until E) {
            val offsetLookupArray : ArrayBuffer[(UInt, UInt)] = ArrayBuffer()
            for (k <- 0 until 4) {
                offsetLookupArray += (k.U -> cache(i)(j)(k))
            }
            entryLookupArray += (j.U -> MuxLookup(offset, 0.U)(offsetLookupArray.toSeq))
        }
        hitDataLookupArray += (i.U -> MuxLookup(hitLineIndex, 0.U)(entryLookupArray.toSeq))
    }
    val hitData = MuxLookup(groupIndex, 0.U)(hitDataLookupArray.toSeq)

    io.io.valid := hitValid || state === s_mem_valid
    io.io.rdata := hitData

    io.mem.araddr := memRAddr
    io.mem.arvalid := ready && !isHit
    
    io.mem.rready := true.B
    io.mem.arlen  := 3.U // BURST 4
    io.mem.arsize := 2.U // 4 bytes per burst
    io.mem.arburst := 1.U // INCR

    io.perf.valid := state === s_mem_valid
    io.perf.isHit := hitValid && state === s_idle && io.io.ready
    io.perf.start := state =/= s_idle && state =/= s_mem_valid
    io.perf.pc    := memRAddr

    // Unused
    io.mem.bready  := DontCare
    io.mem.wdata   := DontCare
    io.mem.wstrb   := DontCare
    io.mem.wvalid  := DontCare
    io.mem.awaddr  := DontCare
    io.mem.awvalid := DontCare
    io.mem.awid    := DontCare
    io.mem.awlen   := DontCare
    io.mem.awsize  := DontCare
    io.mem.awburst := DontCare
    io.mem.wlast   := DontCare
    io.mem.arid    := DontCare
}   
