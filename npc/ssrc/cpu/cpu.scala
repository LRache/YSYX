package cpu

import chisel3._
import chisel3.util.DecoupledIO
import chisel3.util.RegEnable

import cpu.reg.GPR
import cpu.reg.CSR
import cpu.ifu.IFU
import cpu.ifu.ICache
import cpu.idu.IDU
import cpu.exu.EXU
import cpu.lsu.LSU
import cpu.wbu.WBU

import bus.AXI4Arbiter
import bus.AXI4IO


class HCPU(instStart : BigInt) extends Module {
    val io = IO(new Bundle {
        val master  = new AXI4IO
        val slave   = Flipped(new AXI4IO)
        val interrupt = Input(Bool())
    })

    def pipeline_connect[T <: Data](prevOut: DecoupledIO[T], nextIn: DecoupledIO[T]) = {
        prevOut.ready := nextIn.ready
        nextIn.valid := RegEnable(prevOut.valid, false.B, nextIn.ready)
        nextIn.bits := RegEnable(prevOut.bits, prevOut.valid && nextIn.ready)
    }
    
    val gpr = Module(new GPR(Config.GPRAddrLength))
    val csr = Module(new CSR)

    val icache = Module(new ICache(1, 0))
    
    val arbiter = Module(new AXI4Arbiter)
    arbiter.io.sel <> io.master

    val ifu = Module(new IFU(instStart))
    val idu = Module(new IDU)
    val exu = Module(new EXU)
    val lsu = Module(new LSU)
    val wbu = Module(new WBU)
    // pipeline_connect(ifu.io.out, idu.io.in, idu.io.out)
    // ifu.io.out.ready := idu.io.in.ready
    // idu.io.in.bits := ifu.io.out.bits
    // idu.io.in.valid := RegEnable(ifu.io.out.valid, false.B, idu.io.in.ready)
    
    pipeline_connect(ifu.io.out, idu.io.in)
    pipeline_connect(idu.io.out, exu.io.in)
    pipeline_connect(exu.io.out, lsu.io.in)
    // pipeline_connect(lsu.io.out, wbu.io.in)
    // pipeline_connect(wbu.io.out, ifu.io.in)
    lsu.io.out <> wbu.io.in

    // IFU
    icache.io.mem <> arbiter.io.icache
    ifu.io.cache <> icache.io.io

    // IDU
    gpr.io.raddr1 := idu.io.gpr_raddr1
    gpr.io.raddr2 := idu.io.gpr_raddr2
    csr.io.raddr  := idu.io.csr_raddr
    idu.io.gpr_rdata1 := gpr.io.rdata1
    idu.io.gpr_rdata2 := gpr.io.rdata2
    idu.io.csr_rdata  := csr.io.rdata

    // LSU
    lsu.io.mem <> arbiter.io.lsu

    // WBU
    csr.io.waddr := wbu.io.csr_waddr1
    csr.io.is_ecall := exu.io.is_ecall
    csr.io.wdata1 := wbu.io.csr_wdata1
    csr.io.wdata2 := exu.io.csr_wdata2
    gpr.io.waddr := wbu.io.gpr_waddr
    gpr.io.wdata := wbu.io.gpr_wdata
    gpr.io.wen   := wbu.io.gpr_wen

    // ICache
    icache.io.fence := idu.io.fence_i

    // Data Hazard
    def is_raw(raddr: UInt, ren: Bool, waddr: UInt, wen: Bool, valid: Bool) = (waddr === raddr && ren && waddr.orR && wen && valid)
    val exuRaw1 = is_raw(idu.io.gpr_raddr1, idu.io.gpr_ren1, exu.io.out.bits.gpr_waddr, exu.io.out.bits.gpr_wen, exu.io.out.valid)
    val exuRaw2 = is_raw(idu.io.gpr_raddr2, idu.io.gpr_ren2, exu.io.out.bits.gpr_waddr, exu.io.out.bits.gpr_wen, exu.io.out.valid)
    val lsuRaw1 = is_raw(idu.io.gpr_raddr1, idu.io.gpr_ren1, lsu.io.out.bits.gpr_waddr, lsu.io.out.bits.gpr_wen, lsu.io.out.valid)
    val lsuRaw2 = is_raw(idu.io.gpr_raddr2, idu.io.gpr_ren2, lsu.io.out.bits.gpr_waddr, lsu.io.out.bits.gpr_wen, lsu.io.out.valid)
    idu.io.raw := exuRaw1 || exuRaw2
    idu.io.gpr_rdata1 := Mux(lsuRaw1, lsu.io.out.bits.gpr_wdata, gpr.io.rdata1)
    idu.io.gpr_rdata2 := Mux(lsuRaw2, lsu.io.out.bits.gpr_wdata, gpr.io.rdata2)

    // Branch predict
    val predict_failed = exu.io.jmp && exu.io.out.valid
    ifu.io.predict_failed := predict_failed
    idu.io.predict_failed := predict_failed
    ifu.io.dnpc := RegEnable(exu.io.dnpc, exu.io.out.valid)

    io.slave := DontCare

    val debugger = Module(new Dbg())
    debugger.io.clk   := clock
    debugger.io.reset := reset
    debugger.io.brk   := wbu.io.dbg.brk
    debugger.io.ivd   := wbu.io.dbg.ivd
    debugger.io.pc    := wbu.io.dbg.pc
    debugger.io.inst  := wbu.io.dbg.inst
    debugger.io.done  := wbu.io.dbg.done
    debugger.io.gpr_waddr := gpr.io.waddr
    debugger.io.gpr_wdata := gpr.io.wdata
    debugger.io.gpr_wen   := gpr.io.wen

    val counter = Module(new PerfCounter())
    counter.io.ifu_valid := ifu.io.out.valid
    counter.io.icache <> icache.io.perf
    counter.io.lsu <> lsu.io.perf
    counter.io.reset := reset
}

import circt.stage.ChiselStage

object CPU extends App {
    val firtoolOptions = Array("--lowering-options=" + List(
        // make yosys happy
        // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
        "disallowLocalVariables",
        "disallowPackedArrays",
        "locationInfoStyle=wrapInAtSquareBracket"
    ).reduce(_ + "," + _))
    circt.stage.ChiselStage.emitSystemVerilogFile(new HCPU(BigInt(0x30000000L)), args, firtoolOptions)
}
