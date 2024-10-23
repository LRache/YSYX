package cpu.ifu

import chisel3._
import chisel3.util._

import cpu.IFUMessage

import bus.AXI4IO
import cpu.Config

class IFU(instStart : BigInt) extends Module {
    val io = IO(new Bundle {
        val out     = Decoupled(new IFUMessage)
        val cache   = Flipped(new ICacheIO)

        // Branch predict
        val dnpc = Input(UInt(30.W))
        val predict_failed = Input(Bool())
        val is_branch = Input(Bool())
        val is_jmp = Input(Bool())
        val predictor_pc = Input(UInt(30.W))
    })
    val pc   = RegInit(instStart.U(32.W)(31, 2))
    val snpc = pc + 1.U(30.W)
    val dnpc = io.dnpc
    val npc  = Wire(UInt(30.W))

    val s_fetch :: s_skip_once :: Nil = Enum(2)
    val state = RegInit(s_fetch)
    state := MuxLookup(state, s_fetch)(Seq(
        s_fetch     -> Mux(io.predict_failed, s_skip_once, s_fetch),
        s_skip_once -> Mux(io.cache.valid, s_fetch, s_skip_once)
    ))

    if (Config.HasBTB) {
        val btbValid = RegEnable(io.is_jmp, io.is_branch)
        val btbPC  = RegEnable(io.predictor_pc, io.is_branch)
        val btbNPC = RegEnable(dnpc, io.is_branch)
        val predictJmp = btbValid && pc === btbPC
        val pnpc = Mux(predictJmp, btbNPC, snpc)
        npc := Mux(state === s_skip_once || io.predict_failed, Mux(io.is_jmp, dnpc, io.predictor_pc + 1.U(30.W)), pnpc)
        io.out.bits.predict_jmp := predictJmp
    } else {
        npc := Mux(state === s_skip_once || io.predict_failed, dnpc, snpc)
        io.out.bits.predict_jmp := false.B
    }

    io.cache.raddr := pc
    io.cache.ready := true.B

    when(state === s_skip_once) {
        // printf("npc=0x%x snpc=0x%x\n", Cat(npc, 0.U(2.W)), Cat(snpc, 0.U(2.W)))
    }
    pc := Mux(io.out.ready && io.cache.valid, npc, pc)
    val inst = io.cache.rdata

    io.out.bits.pc   := Cat(pc,   0.U(2.W))
    io.out.bits.snpc := Cat(snpc, 0.U(2.W))
    io.out.bits.inst := inst
    
    io.out.valid := io.cache.valid && state === s_fetch && !io.predict_failed
    io.out.bits.dbg.pc   := Cat(pc, 0.U(2.W))
    io.out.bits.dbg.inst := inst
}
