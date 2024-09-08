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

    val icache = Module(new ICache(2, 0))
    
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
    // idu.io.gpr_rdata1 := gpr.io.rdata1
    // idu.io.gpr_rdata2 := gpr.io.rdata2
    csr.io.raddr      := exu.io.csr_raddr
    exu.io.csr_rdata  := csr.io.rdata

    // EXU
    // gpr.io.raddr1 := exu.io.gpr_raddr1
    // gpr.io.raddr2 := exu.io.gpr_raddr2
    // gpr.io.raddr1 := idu.io.gpr_raddr1
    // gpr.io.raddr2 := idu.io.gpr_raddr2
    // exu.io.gpr_rdata1 := Mux(exu.io.gpr_raddr1 === lsu.io.gpr_waddr && lsu.io.gpr_wen && lsu.io.out.valid, lsu.io.gpr_wdata, Mux(exu.io.gpr_raddr1 === wbu.io.gpr_waddr && wbu.io.gpr_wen, wbu.io.gpr_wdata, gpr.io.rdata1))
    // exu.io.gpr_rdata2 := Mux(exu.io.gpr_raddr2 === lsu.io.gpr_waddr && lsu.io.gpr_wen && lsu.io.out.valid, lsu.io.gpr_wdata, Mux(exu.io.gpr_raddr2 === wbu.io.gpr_waddr && wbu.io.gpr_wen, wbu.io.gpr_wdata, gpr.io.rdata2))

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
    def is_raw(raddr: UInt, waddr: UInt, wen: Bool, valid: Bool) = (waddr === raddr && waddr.orR && wen && valid)
    // exu.io.gpr_rdata1 := Mux(exu.io.gpr_raddr1 === lsu.io.gpr_waddr && lsu.io.gpr_wen && lsu.io.out.valid, lsu.io.gpr_wdata, gpr.io.rdata1)
    // exu.io.gpr_rdata2 := Mux(exu.io.gpr_raddr2 === lsu.io.gpr_waddr && lsu.io.gpr_wen && lsu.io.out.valid, lsu.io.gpr_wdata, gpr.io.rdata2)
    val exuRaw1 = is_raw(idu.io.gpr_raddr1, exu.io.out.bits.gpr_waddr, exu.io.out.bits.gpr_wen, exu.io.out.valid)
    val exuRaw2 = is_raw(idu.io.gpr_raddr2, exu.io.out.bits.gpr_waddr, exu.io.out.bits.gpr_wen, exu.io.out.valid)
    val lsuRaw1 = is_raw(idu.io.gpr_raddr1, lsu.io.out.bits.gpr_waddr, lsu.io.out.bits.gpr_wen, lsu.io.out.valid)
    val lsuRaw2 = is_raw(idu.io.gpr_raddr2, lsu.io.out.bits.gpr_waddr, lsu.io.out.bits.gpr_wen, lsu.io.out.valid)
    idu.io.raw := exuRaw1 || exuRaw2
    idu.io.gpr_rdata1 := Mux(lsuRaw1, lsu.io.out.bits.gpr_wdata, gpr.io.rdata1)
    idu.io.gpr_rdata2 := Mux(lsuRaw2, lsu.io.out.bits.gpr_wdata, gpr.io.rdata2)
    
    // when (exuRaw1 || exuRaw2 || lsuRaw1 || lsuRaw2) {
    //     printf("%d %d %d %d\n", exuRaw1, exuRaw2, lsuRaw1, lsuRaw2)
    //     printf("%d %d 0x%x\n", idu.io.gpr_raddr2, exu.io.out.bits.gpr_waddr, idu.io.out.bits.dbg.pc)
    //     assert(0.B)
    // }

    // Branch predict
    // val predict_failed = RegEnable(exu.io.jmp, false.B, exu.io.out.valid)
    val predict_failed = exu.io.jmp && exu.io.out.valid
    ifu.io.predict_failed := predict_failed
    idu.io.predict_failed := predict_failed
    // exu.io.predict_failed := predict_failed
    ifu.io.dnpc := RegEnable(exu.io.dnpc, exu.io.out.valid)
    // assert(!predict_failed)

    io.slave := DontCare

    val debugger = Module(new Dbg())
    debugger.io.clk         := clock
    debugger.io.reset       := reset
    // debugger.io.is_ebreak   := ifu.io.in.bits.is_brk
    // debugger.io.is_invalid  := ifu.io.in.bits.is_ivd
    debugger.io.is_ebreak   := wbu.io.is_brk
    debugger.io.is_invalid  := wbu.io.is_inv
    debugger.io.pc          := wbu.io.dbg_pc
    debugger.io.inst        := ifu.io.out.bits.inst
    debugger.io.valid       := wbu.io.valid

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
