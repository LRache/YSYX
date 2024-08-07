package cpu

import chisel3._

import cpu.reg.RegFile
import cpu.reg.CSR
import cpu.ifu.IFU
import cpu.idu.IDU
import cpu.exu.EXU
import cpu.lsu.LSU
import cpu.wbu.WBU

import bus.AXI4Arbiter
import bus.AXI4IO

class HCPU extends Module {
    val io = IO(new Bundle {
        val master  = new AXI4IO
        val slave   = Flipped(new AXI4IO)
        val interrupt = Input(Bool())
    })
    
    val regFile = Module(new RegFile)
    val csr = Module(new CSR)
    
    val arbiter = Module(new AXI4Arbiter)
    arbiter.io.sel <> io.master

    val ifu = Module(new IFU)
    val idu = Module(new IDU)
    val exu = Module(new EXU)
    val lsu = Module(new LSU)
    val wbu = Module(new WBU)
    idu.io.in <> ifu.io.out
    exu.io.in <> idu.io.out
    lsu.io.in <> exu.io.out
    wbu.io.in <> lsu.io.out
    ifu.io.in <> wbu.io.out

    // IFU
    ifu.io.sram <> arbiter.io.io1

    // IDU
    regFile.io.raddr1 := idu.io.reg_raddr1
    regFile.io.raddr2 := idu.io.reg_raddr2
    idu.io.reg_rdata1 := regFile.io.rdata1
    idu.io.reg_rdata2 := regFile.io.rdata2
    csr.io.raddr      := idu.io.csr_raddr
    idu.io.csr_rdata  := csr.io.rdata

    // LSU
    lsu.io.mem <> arbiter.io.io2

    // WBU
    csr.io.waddr1 := wbu.io.csr_waddr1
    csr.io.waddr2 := wbu.io.csr_waddr2
    csr.io.wdata1 := wbu.io.csr_wdata1
    csr.io.wdata2 := wbu.io.csr_wdata2
    csr.io.wen1   := wbu.io.csr_wen1
    csr.io.wen2   := wbu.io.csr_wen2
    regFile.io.waddr := wbu.io.reg_waddr
    regFile.io.wdata := wbu.io.reg_wdata
    regFile.io.wen   := wbu.io.reg_wen

    val debugger = Module(new Dbg())
    debugger.io.clk         := clock
    debugger.io.reset       := reset
    debugger.io.is_ebreak   := wbu.io.is_brk
    debugger.io.is_invalid  := wbu.io.is_inv
    debugger.io.pc := ifu.io.out.bits.pc
    debugger.io.inst := ifu.io.out.bits.inst
    debugger.io.valid := wbu.io.out.valid

    // slave unused
    io.slave.awready := 0.U
    io.slave.wready  := 0.U
    io.slave.bvalid  := 0.U
    io.slave.bvalid  := 0.U
    io.slave.bresp   := 0.U
    io.slave.bid     := 0.U
    io.slave.arready := 0.U
    io.slave.rvalid  := 0.U
    io.slave.rresp   := 0.U
    io.slave.rdata   := 0.U
    io.slave.rlast   := 0.U
    io.slave.rid     := 0.U
}

import circt.stage.ChiselStage

object CPU extends App {
    (new ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new HCPU)))
}
