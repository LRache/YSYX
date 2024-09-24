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
import cpu.idu.Encode.count


class HCPU(instStart : BigInt) extends Module {
    val io = IO(new Bundle {
        val master  = new AXI4IO
        val slave   = Flipped(new AXI4IO)
        val interrupt = Input(Bool())
    })
    
    val gpr = Module(new GPR(Config.GPRAddrLength))
    val csr = Module(new CSR)
    csr.io.cause_en := false.B
    csr.io.cause := 0.U

    val icache = Module(new ICache(2, 0))
    
    val arbiter = Module(new AXI4Arbiter)
    arbiter.io.sel <> io.master

    def pipeline_connect[T <: Data](prevOut: DecoupledIO[T], nextIn: DecoupledIO[T]) = {
        prevOut.ready := nextIn.ready
        nextIn.valid := RegEnable(prevOut.valid, false.B, nextIn.ready)
        nextIn.bits := RegEnable(prevOut.bits, prevOut.valid && nextIn.ready)
    }

    val ifu = Module(new IFU(instStart))
    val idu = Module(new IDU)
    val exu = Module(new EXU)
    val lsu = Module(new LSU)
    val wbu = Module(new WBU)
    
    pipeline_connect(ifu.io.out, idu.io.in)
    pipeline_connect(idu.io.out, exu.io.in)
    pipeline_connect(exu.io.out, lsu.io.in)
    lsu.io.out <> wbu.io.in

    // EXU
    csr.io.w <> exu.io.csr

    // IFU
    icache.io.mem <> arbiter.io.icache
    ifu.io.cache <> icache.io.io

    // IDU
    gpr.io.raddr1 := idu.io.gpr_raddr1
    gpr.io.raddr2 := idu.io.gpr_raddr2
    csr.io.raddr  := idu.io.csr_raddr
    idu.io.gpr_rdata1 := gpr.io.rdata1
    idu.io.gpr_rdata2 := gpr.io.rdata2

    // LSU
    lsu.io.mem <> arbiter.io.lsu

    // WBU
    gpr.io.waddr := wbu.io.in.bits.gpr_waddr
    gpr.io.wdata := wbu.io.in.bits.gpr_wdata
    gpr.io.wen   := wbu.io.in.valid

    // ICache
    icache.io.fence := idu.io.fence_i

    // Data Hazard
    def raw_con(valid: Bool, waddr: UInt, wen: Bool = true.B): Bool = {
        valid && waddr.orR && wen
    }
    def is_raw(raddr: UInt, ren: Bool, waddr: UInt, con: Bool): Bool = {
        waddr === raddr && ren && con
    }
    
    val exuGPRWaddr = exu.io.out.bits.gpr_waddr
    val exuRawGPRCon = raw_con(
        exu.io.out.valid,
        exuGPRWaddr
    )
    
    val exuRaw1 = is_raw(idu.io.gpr_raddr1, idu.io.gpr_ren1, exuGPRWaddr, exuRawGPRCon)
    val exuRaw2 = is_raw(idu.io.gpr_raddr2, idu.io.gpr_ren2, exuGPRWaddr, exuRawGPRCon)
    val exuRawCantSolve1 = exuRaw1 && exu.io.gprWSel
    val exuRawCantSolve2 = exuRaw2 && exu.io.gprWSel
    val exuRawSolveable1 = exuRaw1 && !exu.io.gprWSel
    val exuRawSolveable2 = exuRaw2 && !exu.io.gprWSel
    if (Config.JudgeExuRaw) {
        idu.io.raw := exuRawCantSolve1 || exuRawCantSolve2
    } else {
        idu.io.raw := exuRaw1 || exuRaw2
    }

    val lsuGPRWaddr = lsu.io.out.bits.gpr_waddr
    val lsuRawCon = raw_con(
        lsu.io.out.valid,
        lsuGPRWaddr
    )
    val lsuRaw1 = is_raw(idu.io.gpr_raddr1, true.B, lsuGPRWaddr, lsuRawCon)
    val lsuRaw2 = is_raw(idu.io.gpr_raddr2, true.B, lsuGPRWaddr, lsuRawCon)
    if (Config.JudgeExuRaw) {
        idu.io.gpr_rdata1 := Mux(exuRawSolveable1, exu.io.out.bits.rs, Mux(lsuRaw1, lsu.io.out.bits.gpr_wdata, gpr.io.rdata1))
        idu.io.gpr_rdata2 := Mux(exuRawSolveable2, exu.io.out.bits.rs, Mux(lsuRaw2, lsu.io.out.bits.gpr_wdata, gpr.io.rdata2))
    } else {
        idu.io.gpr_rdata1 := Mux(lsuRaw1, lsu.io.out.bits.gpr_wdata, gpr.io.rdata1)
        idu.io.gpr_rdata2 := Mux(lsuRaw2, lsu.io.out.bits.gpr_wdata, gpr.io.rdata2)
    }

    val exuRawCSRCon = raw_con(
        exu.io.out.valid,
        exuGPRWaddr,
        exu.io.csr.wen
    )
    val exuRawCSR = is_raw(idu.io.csr_raddr, idu.io.csr_ren, exu.io.csr.waddr, exuRawCSRCon)
    idu.io.csr_rdata := Mux(exuRawCSR, exu.io.csr.wdata, csr.io.rdata)

    // Branch predict
    val predict_failed = (exu.io.jmp && exu.io.out.valid) || idu.io.fence_i
    ifu.io.predict_failed := predict_failed
    idu.io.predict_failed := predict_failed
    ifu.io.dnpc := RegEnable(exu.io.dnpc, exu.io.out.valid)

    io.slave := DontCare

    // Debugger
    if (Config.HasDBG) {
        val debugger = Module(new Dbg())
        debugger.io.clk   := clock
        debugger.io.reset := reset
        debugger.io.brk   := wbu.io.dbg.brk
        debugger.io.ivd   := wbu.io.dbg.ivd
        debugger.io.pc    := wbu.io.dbg.pc
        debugger.io.inst  := wbu.io.dbg.inst
        debugger.io.done  := wbu.io.dbg.done
        debugger.io.gpr.waddr := gpr.io.waddr
        debugger.io.gpr.wen   := gpr.io.wen
        debugger.io.gpr.wdata := gpr.io.wdata
        debugger.io.csr := wbu.io.dbg.csr

        val counter = Module(new PerfCounter())
        counter.io.ifu_valid := ifu.io.out.valid
        counter.io.idu_ready := idu.io.in.ready
        counter.io.exu_valid := exu.io.out.valid
        counter.io.icache <> icache.io.perf
        counter.io.lsu <> lsu.io.perf
        counter.io.branch_predict_failed := predict_failed
        counter.io.branch_predict_success := (!predict_failed) && lsu.io.in.ready
        counter.io.reset := reset
        counter.io.clk   := clock.asBool
    }
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
