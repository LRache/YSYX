package cpu

import chisel3._
import chisel3.util.DecoupledIO
import chisel3.util.Decoupled
import chisel3.util.RegEnable

import cpu.reg.GPR
import cpu.reg.CSR
import cpu.ifu.IFU
import cpu.ifu.ICache
import cpu.idu.IDU
import cpu.exu.EXU
import cpu.lsu.LSU
import cpu.wbu.WBU
import cpu.Config

import bus.AXI4Arbiter
import bus.AXI4IO
import cpu.reg.CSRAddr
import bus.ClintInline
import bus.AXI4ArbiterInline
import cpu.idu.IDUInline
import cpu.idu.IDUWire

class HCPU(instStart : BigInt) extends Module {
    val io = IO(new Bundle {
        val master  = new AXI4IO
        val slave   = Flipped(new AXI4IO)
        val interrupt = Input(Bool())
    })
    val gpr = Module(new GPR(Config.GPRAddrLength))
    val csr = Module(new CSR)

    val icache = Module(new ICache(2, 0))

    def pipeline_connect[T <: Data](prevOut: DecoupledIO[T], nextIn: DecoupledIO[T]) = {
        prevOut.ready := nextIn.ready
        nextIn.valid := RegEnable(prevOut.valid, false.B, nextIn.ready)
        nextIn.bits := RegEnable(prevOut.bits, prevOut.valid && nextIn.ready)
    }

    val idu = Wire(new IDUWire)
    IDUInline(idu)

    val ifu = Module(new IFU(instStart))
    // val idu = Module(new IDU)
    val exu = Module(new EXU)
    val lsu = Module(new LSU)
    val wbu = Module(new WBU)
    
    pipeline_connect(ifu.io.out, idu.in)
    pipeline_connect(idu.out, exu.io.in)
    pipeline_connect(exu.io.out, lsu.io.in)
    lsu.io.out <> wbu.io.in

    val arbiterSel = Wire(Flipped(new AXI4IO))
    arbiterSel <> io.master
    AXI4ArbiterInline(
        icache = icache.io.mem,
        lsu = lsu.io.mem,
        sel = arbiterSel,
    )

    // EXU
    csr.io.w <> exu.io.csr
    csr.io.trap := exu.io.trap

    // IFU
    ifu.io.cache <> icache.io.io

    // IDU
    gpr.io.raddr1 := idu.gpr_raddr1
    gpr.io.raddr2 := idu.gpr_raddr2
    csr.io.raddr  := idu.csr_raddr
    idu.csr_rdata := csr.io.rdata

    // LSU

    // WBU
    gpr.io.waddr := wbu.io.in.bits.gpr_waddr
    gpr.io.wdata := wbu.io.in.bits.gpr_wdata
    gpr.io.wen   := wbu.io.in.valid

    // ICache
    icache.io.fence := idu.fence_i

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
    
    // val exuRaw1 = is_raw(idu.io.gpr_raddr1, idu.io.gpr_ren1, exuGPRWaddr, exuRawGPRCon)
    // val exuRaw2 = is_raw(idu.io.gpr_raddr2, idu.io.gpr_ren2, exuGPRWaddr, exuRawGPRCon)
    val exuRaw1 = is_raw(idu.gpr_raddr1, idu.gpr_ren1, exuGPRWaddr, exuRawGPRCon)
    val exuRaw2 = is_raw(idu.gpr_raddr2, idu.gpr_ren2, exuGPRWaddr, exuRawGPRCon)
    val exuRawCantSolve1 = exuRaw1 &&  exu.io.gprWSel
    val exuRawCantSolve2 = exuRaw2 &&  exu.io.gprWSel
    val exuRawSolveable1 = exuRaw1 && !exu.io.gprWSel
    val exuRawSolveable2 = exuRaw2 && !exu.io.gprWSel

    val lsuGPRWaddr = lsu.io.out.bits.gpr_waddr
    val lsuRawCon = raw_con(
        lsu.io.out.valid,
        lsuGPRWaddr
    )
    // val lsuRaw1 = is_raw(idu.io.gpr_raddr1, true.B, lsuGPRWaddr, lsuRawCon)
    // val lsuRaw2 = is_raw(idu.io.gpr_raddr2, true.B, lsuGPRWaddr, lsuRawCon)
    val lsuRaw1 = is_raw(idu.gpr_raddr1, true.B, lsuGPRWaddr, lsuRawCon)
    val lsuRaw2 = is_raw(idu.gpr_raddr2, true.B, lsuGPRWaddr, lsuRawCon)
    if (Config.JudgeExuRaw) {
        // idu.io.gpr_rdata1 := Mux(exuRawSolveable1, exu.io.out.bits.rs, Mux(lsuRaw1, lsu.io.out.bits.gpr_wdata, gpr.io.rdata1))
        // idu.io.gpr_rdata2 := Mux(exuRawSolveable2, exu.io.out.bits.rs, Mux(lsuRaw2, lsu.io.out.bits.gpr_wdata, gpr.io.rdata2))
        idu.gpr_rdata1 := Mux(exuRawSolveable1, exu.io.out.bits.rs, Mux(lsuRaw1, lsu.io.out.bits.gpr_wdata, gpr.io.rdata1))
        idu.gpr_rdata2:= Mux(exuRawSolveable2, exu.io.out.bits.rs, Mux(lsuRaw2, lsu.io.out.bits.gpr_wdata, gpr.io.rdata2))
    } else {
        // idu.io.gpr_rdata1 := Mux(lsuRaw1, lsu.io.out.bits.gpr_wdata, gpr.io.rdata1)
        // idu.io.gpr_rdata2 := Mux(lsuRaw2, lsu.io.out.bits.gpr_wdata, gpr.io.rdata2)
        idu.gpr_rdata1 := Mux(lsuRaw1, lsu.io.out.bits.gpr_wdata, gpr.io.rdata1)
        idu.gpr_rdata2 := Mux(lsuRaw2, lsu.io.out.bits.gpr_wdata, gpr.io.rdata2)
    }

    val exuRawCSRCon = raw_con(
        exu.io.out.valid,
        true.B,
        exu.io.csr.wen
    )
    // val exuRawCSR = is_raw(idu.io.csr_raddr, idu.io.csr_ren, exu.io.csr.waddr, exuRawCSRCon)
    // val exuRawTrap = exu.io.out.valid && exu.io.trap.is_trap && idu.io.csr_ren &&
    //  (idu.io.csr_raddr === CSRAddr.MCAUSE || idu.io.csr_raddr === CSRAddr.MEPC)
    val exuRawCSR = is_raw(csr.io.raddr, idu.csr_ren, exu.io.csr.waddr, exuRawCSRCon)
    val exuRawTrap = exu.io.out.valid && exu.io.trap.is_trap && idu.csr_ren &&
     (csr.io.raddr === CSRAddr.MCAUSE || csr.io.raddr === CSRAddr.MEPC)

    if (Config.JudgeExuRaw) {
        // raw := exuRawCantSolve1 || exuRawCantSolve2
        idu.raw := exuRawCantSolve1 || exuRawCantSolve2
    } else {
        // raw := exuRaw1 || exuRaw2 || exuRawCSR || exuRawTrap
        idu.raw := exuRaw1 || exuRaw2 || exuRawCSR || exuRawTrap
    }

    // Branch predict
    // val predict_failed = (exu.io.jmp && exu.io.out.valid) || idu.io.fence_i
    val predict_failed = ((exu.io.jmp ^ exu.io.predict_jmp) && exu.io.out.valid)
    val is_fence = idu.fence_i && idu.out.valid
    idu.predict_failed := predict_failed
    // idu.io.predict_failed := predict_failed
    ifu.io.predict_failed := predict_failed || is_fence
    ifu.io.predictor_pc := RegEnable(exu.io.predictor_pc,       exu.io.out.valid)
    ifu.io.dnpc      := RegEnable(exu.io.dnpc,                   exu.io.out.valid)
    ifu.io.is_branch := RegEnable(exu.io.is_branch,              exu.io.out.valid)
    ifu.io.is_jmp    := RegEnable(exu.io.jmp, exu.io.out.valid)
    ifu.io.is_fence  := RegEnable(icache.io.fence, idu.out.valid)
    
    // CLINT
    if (Config.HasClint) {
        val clintRData = Wire(UInt(32.W))
        ClintInline(
            raddr = arbiterSel.araddr(2),
            rdata = clintRData
        )

        val loadClint = arbiterSel.araddr(31, 24) === 0x02.U
        arbiterSel.arready := Mux(loadClint, true.B, io.master.arready)
        arbiterSel.rvalid  := Mux(loadClint, true.B, io.master.rvalid)
        arbiterSel.rdata   := Mux(loadClint, clintRData, io.master.rdata)
        arbiterSel.rlast   := Mux(loadClint, true.B, io.master.rlast)
        io.master.arvalid  := Mux(loadClint, false.B, arbiterSel.arvalid)
    }

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
        debugger.io.gpr.waddr := wbu.io.in.bits.gpr_waddr
        debugger.io.gpr.wen   := wbu.io.in.valid
        debugger.io.gpr.wdata := wbu.io.in.bits.gpr_wdata
        debugger.io.csr := wbu.io.dbg.csr
        
        debugger.io.is_trap := wbu.io.dbg.is_trap
        debugger.io.cause := wbu.io.dbg.cause
        debugger.io.epc := wbu.io.dbg.pc
        
        debugger.io.branch_predict_failed := predict_failed && lsu.io.in.ready
        debugger.io.branch_predict_success := (!predict_failed) && lsu.io.in.ready
        debugger.io.exu_valid := exu.io.out.valid

        val counter = Module(new PerfCounter())
        counter.io.ifu_valid := ifu.io.out.valid
        // counter.io.idu_ready := idu.io.in.ready
        counter.io.idu_ready := idu.in.ready
        counter.io.exu_valid := exu.io.out.valid
        counter.io.icache <> icache.io.perf
        counter.io.lsu <> lsu.io.perf
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
