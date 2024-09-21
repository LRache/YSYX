package cpu.ifu

import chisel3._
import chisel3.util._

import cpu.IFUMessage

import bus.AXI4IO

class IFU(instStart : BigInt) extends Module {
    val io = IO(new Bundle {
        val out     = Decoupled(new IFUMessage)
        val cache   = Flipped(new ICacheIO)

        // Branch
        val dnpc = Input(UInt(32.W))
        val predict_failed = Input(Bool())
    })
    val dnpc = io.dnpc
    val pc   = RegInit(instStart.U(32.W))
    val snpc = pc + 4.U(32.W)

    val s_fetch :: s_skip_once :: Nil = Enum(2)
    val state = RegInit(s_fetch)
    state := MuxLookup(state, s_fetch)(Seq(
        s_fetch -> Mux(io.predict_failed, s_skip_once, s_fetch),
        s_skip_once -> Mux(io.cache.valid, s_fetch, s_skip_once)
    ))

    io.cache.raddr := pc
    io.cache.ready := true.B
    val npc = Mux(state === s_skip_once, dnpc, snpc)
    pc := Mux(io.out.ready && io.cache.valid, npc, pc)
    val inst = io.cache.rdata
    
    io.out.bits.pc   := pc
    io.out.bits.snpc := snpc
    io.out.bits.inst := inst
    
    io.out.valid := io.cache.valid && state === s_fetch && !io.predict_failed
    io.out.bits.dbg.pc   := pc
    io.out.bits.dbg.inst := inst
}
