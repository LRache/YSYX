package cpu

import chisel3._
import cpu.Config.GPRAddrLength
import cpu.Config.CSRAddrLength

class IFUMessage extends Bundle {
    val inst = Output(UInt(32.W))
    val pc   = Output(UInt(32.W))
    val snpc = Output(UInt(32.W))

    val dbg = new Bundle {
        val pc = UInt(32.W)
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
    val is_branch = Output(Bool())
    val is_jmp  = Output(Bool())
    val dnpc_sel   = Output(Bool())

    // LSU
    val mem_wen    = Output(Bool())
    val mem_ren    = Output(Bool())
    
    // WBU
    val gpr_waddr  = Output(UInt(GPRAddrLength.W))
    val gpr_wen    = Output(Bool())
    val gpr_ws     = Output(UInt(3.W))

    val csr_waddr  = Output(UInt(CSRAddrLength.W))
    val is_ecall   = Output(Bool())
    // val csr_wd_sel = Output(Bool())
    // val csr_ws     = Output(UInt(3.W))
    // val snpc       = Output(UInt(32.W))

    val is_brk     = Output(Bool())
    val is_ivd     = Output(Bool())

    val dbg = new Bundle {
        val pc = Output(UInt(32.W))
        // val inst = Output(UInt(32.W))
    }
}

class EXUMessage extends Bundle {
    // val pc_sel     = Bool()
    val exu_result = Output(UInt(32.W))
    // val dnpc = UInt(32.W)

    val gpr_wdata = Output(UInt(32.W))
    val csr_wdata = Output(UInt(32.W))
    // val csr_wdata2 = UInt(32.W)

    // Passthrough
    val func3   = Output(UInt(3.W))
    val mem_wen = Output(Bool())
    val mem_ren = Output(Bool())
        
    val gpr_waddr  = Output(UInt(GPRAddrLength.W))
    val gpr_rdata2 = Output(UInt(32.W))
    val gpr_wen    = Output(Bool())
    val gpr_ws     = Output(UInt(3.W))

    val csr_waddr  = Output(UInt(4.W))
    val is_ecall   = Output(Bool())

    val is_brk     = Output(Bool())
    val is_ivd     = Output(Bool())

    val dbg = new Bundle {
        val pc = UInt(32.W)
    }
}

class LSUMessage extends Bundle {
    val gpr_wdata  = Output(UInt(32.W))

    // Passthrough        
    val gpr_waddr  = Output(UInt(GPRAddrLength.W))
    val gpr_wen    = Output(Bool())

    val csr_waddr  = Output(UInt(4.W))
    val is_ecall   = Output(Bool())
    val csr_wdata  = Output(UInt(32.W))

    val is_brk = Output(Bool())
    val is_ivd = Output(Bool())

    val dbg = new Bundle {
        val pc   = Output(UInt(32.W))
        // val inst = Output(UInt(32.W))
    }
}

class WBUMessage extends Bundle {
    // val dnpc = UInt(32.W)
    // val pc_sel = Bool()

    // val is_brk     = Bool()
    // val is_ivd     = Bool()

    val dbg = new Bundle {
        val pc = UInt(32.W)
    }
}
