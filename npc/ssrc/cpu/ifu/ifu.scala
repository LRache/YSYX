package cpu.ifu

import chisel3._
import chisel3.util._

import cpu.IFUMessage

import bus.AXI4IO
import cpu.Config
import cpu.idu.ImmType.B

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
    val dnpc = io.dnpc
    val pc   = RegInit(instStart.U(32.W)(31, 2))
    val snpc = Wire(UInt(30.W))

    if (Config.HasBTB) {
        val btbWen = io.is_branch && io.is_jmp
        val btbValid = RegEnable(io.is_jmp, btbWen)
        val btbPC  = RegEnable(io.predictor_pc, btbWen)
        val btbNPC = RegEnable(io.dnpc, btbWen)
        val predictJmp = btbValid && pc === btbPC
        snpc := Mux(predictJmp, btbNPC, pc + 1.U(30.W))
        when (!(Cat(snpc, 0.U(2.W)) >= 0x30000000L.U && Cat(snpc, 0.U(2.W)) <= 0x30000100L.U)) {
            printf("%x\n", Cat(snpc, 0.U(2.W)))
        }
        assert((Cat(snpc, 0.U(2.W)) >= 0x30000000L.U && Cat(snpc, 0.U(2.W)) <= 0x30000100L.U))
        io.out.bits.predict_jmp := predictJmp
    } else {
        snpc := pc + 1.U(30.W)
        io.out.bits.predict_jmp := false.B
    }

    val s_fetch :: s_skip_once :: Nil = Enum(2)
    val state = RegInit(s_fetch)
    state := MuxLookup(state, s_fetch)(Seq(
        s_fetch -> Mux(io.predict_failed, s_skip_once, s_fetch),
        s_skip_once -> Mux(io.cache.valid, s_fetch, s_skip_once)
    ))

    val apc = Cat(pc, 0.U(2.W))
    assert(apc >= 0x30000000L.U && apc <= 0x30000100L.U)

    io.cache.raddr := pc
    io.cache.ready := true.B
    val npc = Mux(state === s_skip_once, dnpc, snpc)
    pc := Mux(io.out.ready && io.cache.valid, npc, pc)
    val inst = io.cache.rdata

    io.out.bits.pc   := Cat(pc,   0.U(2.W))
    io.out.bits.snpc := Cat(snpc, 0.U(2.W))
    io.out.bits.inst := inst
    
    io.out.valid := io.cache.valid && state === s_fetch && !io.predict_failed
    io.out.bits.dbg.pc   := Cat(pc, 0.U(2.W))
    io.out.bits.dbg.inst := inst
}
