package cpu.ifu

import chisel3._
import chisel3.util._

import cpu.WBUMessage
import cpu.IFUMessage

import bus.AXI4IO

class IFU(instStart : BigInt) extends Module {
    val io = IO(new Bundle {
        // val in      = Flipped(Decoupled(new WBUMessage))
        val out     = Decoupled(new IFUMessage)
        val cache   = Flipped(new ICacheIO)

        // Branch
        val dnpc = Input(UInt(32.W))
        val predict_failed = Input(Bool())
    })
    // val s_wait_cache :: s_valid :: Nil = Enum(2)
    // val state = RegInit(s_wait_cache)
    // state := MuxLookup(state, s_wait_cache)(Seq (
    //     s_wait_cache -> Mux(io.cache.valid && io.out.ready, s_valid, s_wait_cache),
    //     // s_valid      -> Mux(io.in.valid && io.out.ready, s_wait_cache, s_valid)
    //     s_valid -> s_wait_cache
    // ))

    val pc   = RegInit(instStart.U(32.W))
    val snpc = pc + 4.U(32.W)
    
    // io.cache.raddr := pc
    // io.cache.ready := state === s_wait_cache

    // val valid = state === s_wait_cache && io.cache.valid && io.out.ready
    // pc := Mux(state === s_valid && io.out.ready, snpc, pc)
    // val inst = RegEnable(io.cache.rdata, valid)

    val s_fetch :: s_skip_once :: Nil = Enum(2)
    val state = RegInit(s_fetch)
    state := MuxLookup(state, s_fetch)(Seq(
        s_fetch -> Mux(io.predict_failed, s_skip_once, s_fetch),
        s_skip_once -> Mux(io.cache.valid, s_fetch, s_skip_once)
    ))

    io.cache.raddr := pc
    io.cache.ready := true.B
    // pc := Mux(io.cache.valid && io.out.ready, snpc, pc)
    pc := Mux(io.out.ready && io.cache.valid, Mux(state === s_skip_once, io.dnpc, snpc), pc)
    
    io.out.bits.pc   := pc
    io.out.bits.snpc := snpc
    io.out.bits.inst := io.cache.rdata
    
    io.out.valid := io.cache.valid && state === s_fetch && !io.predict_failed
    // io. in.ready := true.B
    // when(io.out.valid) {
    //     printf("IFU 0x%x\n", io.out.bits.pc)
    // }
    io.out.bits.dbg.pc := pc
}
