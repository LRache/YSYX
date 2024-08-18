package cpu.ifu

import chisel3._
import chisel3.util._

import cpu.WBUMessage
import cpu.IFUMessage

import bus.AXI4IO

class IFU (instStart : BigInt) extends Module {
    val io = IO(new Bundle {
        val in      = Flipped(Decoupled(new WBUMessage))
        val out     = Decoupled(new IFUMessage)
        val mem     = new AXI4IO

        val dbg0 = Output(UInt(32.W))
        val dbg1 = Output(UInt(32.W))
        val dbg2 = Output(UInt(32.W))

        val dbg_read_valid = Output(Bool())
    })
    val pc   = RegInit(instStart.U(32.W))
    val snpc = pc + 4.U(32.W)
    val inst = RegInit(0.U(32.W))

    pc := Mux(io.in.valid, Mux(io.in.bits.pc_sel, io.in.bits.dnpc, snpc), pc)
    
    val s_wait_arready :: s_wait_rvalid :: s_valid :: Nil = Enum(3)
    val state = RegInit(s_wait_arready)

    state := MuxLookup(state, s_wait_arready)(Seq (
        s_wait_arready -> Mux(io.mem.arready, s_wait_rvalid,  s_wait_arready),
        s_wait_rvalid  -> Mux(io.mem. rvalid, s_valid,        s_wait_rvalid ),
        s_valid        -> Mux(io.in.valid    ,s_wait_arready, s_valid       ) 
    ))
    inst := Mux(state === s_wait_rvalid, io.mem.rdata, inst);

    io.mem.araddr  := pc
    io.mem.arvalid := state === s_wait_arready
    io.mem. rready := state === s_wait_rvalid
    io.mem.arsize  := 4.U
    io.dbg_read_valid := state === s_valid

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
    io.mem.arburst := 0.U
    io.mem.arid    := 0.U
    io.mem.arlen   := 0.U
    
    io.out.bits.pc   := pc
    io.out.bits.snpc := snpc
    io.out.bits.inst := inst
    
    io.out.valid := state === s_valid
    io. in.ready := true.B
    
    io.dbg0 := 0.U
    io.dbg1 := state
    io.dbg2 := io.mem.rvalid
}
