package cpu.ifu

import chisel3._
import chisel3.util._

import cpu.WBUMessage
import cpu.IFUMessage

import bus.AXI4IO

class IFU(instStart : BigInt) extends Module {
    val io = IO(new Bundle {
        val in      = Flipped(Decoupled(new WBUMessage))
        val out     = Decoupled(new IFUMessage)
        val cache   = Flipped(new ICacheIO)

        val dbg_read_valid = Output(Bool())
    })
    val pc   = RegInit(instStart.U(32.W))
    val snpc = pc + 4.U(32.W)
    val inst = RegInit(0.U(32.W))
    
    val s_wait_cache :: s_valid :: Nil = Enum(2)
    val state = RegInit(s_wait_cache)
    state := MuxLookup(state, s_wait_cache)(Seq (
        s_wait_cache -> Mux(io.cache.valid, s_valid, s_wait_cache),
        s_valid      -> Mux(io.in.valid, s_wait_cache, s_valid)
    ))
    
    io.cache.raddr := pc
    io.cache.ready := state === s_wait_cache

    pc := Mux(io.in.valid, Mux(io.in.bits.pc_sel, io.in.bits.dnpc, snpc), pc)
    inst := Mux(state === s_wait_cache && io.cache.valid, io.cache.rdata, inst)
    
    io.dbg_read_valid := state === s_valid
    
    io.out.bits.pc   := pc
    io.out.bits.snpc := snpc
    io.out.bits.inst := inst
    
    io.out.valid := state === s_valid
    io. in.ready := true.B
}
