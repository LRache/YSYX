package cpu.ifu

import chisel3._
import chisel3.util._

import cpu.WBUMessage
import cpu.IFUMessage

import bus.AXI4IO

class IFU extends Module {
    val io = IO(new Bundle {
        val in      = Flipped(Decoupled(new WBUMessage))
        val out     = Decoupled(new IFUMessage)
        val sram    = new AXI4IO

        val dbg0 = Output(UInt(32.W))
        val dbg1 = Output(UInt(32.W))
        val dbg2 = Output(UInt(32.W))
    })
    val fisrtInstTag = RegInit(true.B)
    val pcReg = RegInit(0x20000000L.U(32.W))
    val snpc = pcReg + 4.U(32.W)
    val inst = RegInit(0.U(32.W))
    
    when (!fisrtInstTag) {
        when (io.in.valid) {
            pcReg := Mux(io.in.bits.pc_sel, io.in.bits.dnpc, snpc)
        }
    }.otherwise {
        fisrtInstTag := false.B
    }
    
    val s_wait_arready :: s_wait_rvalid :: s_valid :: Nil = Enum(3)
    val state = RegInit(s_wait_arready)

    state := MuxLookup(state, s_wait_arready)(Seq (
        s_wait_arready -> Mux(io.sram.arready, s_wait_rvalid,  s_wait_arready),
        s_wait_rvalid  -> Mux(io.sram. rvalid, s_valid,        s_wait_rvalid ),
        s_valid        -> Mux(io.in.valid    , s_wait_arready, s_valid       ) 
    ))
    inst := Mux(state === s_wait_rvalid, io.sram.rdata, inst);

    io.sram.araddr  := pcReg
    io.sram.arvalid := state === s_wait_arready
    io.sram. rready := state === s_wait_rvalid
    io.sram.arsize  := 4.U

    // Unused
    io.sram.bready  := false.B
    io.sram.wdata   := 0.U
    io.sram.wstrb   := 0.U
    io.sram.wvalid  := 0.U
    io.sram.awaddr  := 0.U
    io.sram.awvalid := 0.U
    io.sram.awid    := 0.U
    io.sram.awlen   := 0.U
    io.sram.awsize  := 0.U
    io.sram.awburst := 0.U
    io.sram.wlast   := false.B
    io.sram.arburst := 0.U
    io.sram.arid    := 0.U
    io.sram.arlen   := 0.U
    
    io.out.bits.pc   := pcReg
    io.out.bits.snpc := snpc
    io.out.bits.inst := inst
    
    io.out.valid := state === s_valid
    io. in.ready := true.B
    
    io.dbg0 := 0.U
    io.dbg1 := state
    io.dbg2 := io.sram.rvalid
}
