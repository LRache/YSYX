package cpu

import chisel3._
import cpu.Config

class RegWIO (addrLength: Int) extends Bundle {
    val waddr = Output(UInt(addrLength.W))
    val wen   = Output(Bool())
    val wdata = Output(UInt(32.W))
}

class RegRIO (addrLength: Int) extends Bundle {
    val raddr = Output(UInt(addrLength.W))
    val rdata = Input (UInt(32.W))
}

class IFUMessage extends Bundle {
    val inst = Output(UInt(32.W))
    val pc   = Output(UInt(32.W))
    val snpc = Output(UInt(32.W))

    val dbg = new Bundle {
        val pc   = Output(UInt(32.W))
        val inst = Output(UInt(32.W))
    }
}

class IDUMessage extends Bundle {
    // EXU
    val func3   = Output(UInt(3.W))
    val rs1     = Output(UInt(32.W))
    val rs2     = Output(UInt(32.W))
    val rs3     = Output(UInt(32.W))
    val rs4     = Output(UInt(32.W))
    val exu_tag = Output(Bool())
    val alu_bsel= Output(Bool())
    val alu_add = Output(Bool())
    val is_branch = Output(Bool())
    val is_jmp  = Output(Bool())
    val dnpc_sel   = Output(Bool())

    // LSU
    val mem_wen    = Output(Bool())
    val mem_ren    = Output(Bool())
    
    // WBU
    val gpr_waddr  = Output(UInt(Config.GPRAddrLength.W))
    val gpr_wen    = Output(Bool())
    val gpr_ws     = Output(UInt(2.W))

    val csr_waddr  = Output(UInt(Config.CSRAddrLength.W))
    val csr_wen    = Output(Bool())
    val cause_en   = Output(Bool())

    val is_brk     = Output(Bool())
    val is_ivd     = Output(Bool())

    val dbg = new Bundle {
        val pc   = Output(UInt(32.W))
        val inst = Output(UInt(32.W))
    }
}

class EXUMessage extends Bundle {
    val rs = Output(UInt(32.W))

    // Passthrough
    val func3   = Output(UInt(3.W))
    val mem_wen = Output(Bool())
    val mem_ren = Output(Bool())
    val mem_wdata = Output(UInt(32.W))
        
    val gpr_waddr  = Output(UInt(Config.GPRAddrLength.W))
    val gpr_wen    = Output(Bool())
    val gpr_ws     = Output(UInt(2.W))

    val is_brk     = Output(Bool())
    val is_ivd     = Output(Bool())

    val dbg = new Bundle {
        val pc   = Output(UInt(32.W))
        val inst = Output(UInt(32.W))
        val csr  = Output(new RegWIO(32))
    }
}

class LSUMessage extends Bundle {
    val gpr_wdata  = Output(UInt(32.W))

    // Passthrough        
    val gpr_waddr  = Output(UInt(Config.GPRAddrLength.W))
    val gpr_wen    = Output(Bool())

    val is_brk = Output(Bool())
    val is_ivd = Output(Bool())

    val dbg = new Bundle {
        val pc   = Output(UInt(32.W))
        val inst = Output(UInt(32.W))
        val csr  = Output(new RegWIO(32))
    }
}
