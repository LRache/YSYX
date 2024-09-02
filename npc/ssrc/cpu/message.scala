package cpu

import chisel3._
import cpu.Config.GPRAddrLength

class IFUMessage extends Bundle {
    val inst = UInt(32.W)
    val pc   = UInt(32.W)
    val snpc = UInt(32.W)
}

class IDUMessage extends Bundle {
    val rs1        = UInt(32.W)
    val rs2        = UInt(32.W)
    val imm        = UInt(32.W)
    val a_sel      = Bool()
    val b_sel      = Bool()
    val alu_sel    = UInt(4.W)
    val cmp_sel    = UInt(3.W)
    val is_jmp     = Bool()
    val csr_rdata  = UInt(32.W)
    val pc         = UInt(32.W)

    val mem_wen    = Bool()
    val mem_ren    = Bool()
    val mem_type   = UInt(3.W)
        
    val rd         = UInt(GPRAddrLength.W)
    val reg_wen    = Bool()
    val reg_ws     = UInt(3.W)

    val csr_waddr1 = UInt(4.W)
    // val csr_waddr2 = UInt(12.W)
    val is_ecall   = Bool()
    val csr_wen1   = Bool()
    // val csr_wen2   = UInt(32.W)
    val csr_wd_sel = Bool()
    val csr_ws     = UInt(3.W)
    val csr_imm    = UInt(5.W)
    val snpc       = UInt(32.W)
    
    val dnpc_sel   = Bool()

    val is_brk     = Bool()
    val is_ivd     = Bool()
}

class EXUMessage extends Bundle {
    val pc_sel     = Bool()
    val exu_result = UInt(32.W)

    val csr_wdata1 = UInt(32.W)
    // val csr_wdata2 = UInt(32.W)

    // Passthrough
    val mem_wen    = Bool()
    val mem_ren    = Bool()
    val mem_type   = UInt(3.W)
        
    val rd         = UInt(GPRAddrLength.W)
    val rs2        = UInt(32.W)
    val gpr_wen    = Bool()
    val gpr_ws     = UInt(3.W)

    val csr_waddr1 = UInt(4.W)
    val is_ecall   = Bool()
    // val csr_waddr2 = UInt(12.W)
    val csr_wen1   = Bool()
    // val csr_wen2   = UInt(32.W)
    val csr_wd_sel = Bool()
    // val csr_ws     = UInt(3.W)
    val csr_imm    = UInt(5.W)
    val csr_rdata  = UInt(32.W)
    val snpc       = UInt(32.W)
    // val pc         = UInt(32.W)
    
    val dnpc = UInt(32.W)

    val is_brk     = Bool()
    val is_ivd     = Bool()
}

class LSUMessage extends Bundle {
    // val mem_rdata  = UInt(32.W)
    val gpr_wdata  = UInt(32.W)

    // Passthrough
    val pc_sel     = Bool()
    // val exu_result = UInt(32.W)
        
    val rd         = UInt(GPRAddrLength.W)
    val gpr_wen    = Bool()
    // val reg_ws     = UInt(3.W)

    val csr_waddr1 = UInt(4.W)
    val is_ecall   = Bool()
    // val csr_waddr2 = UInt(12.W)
    val csr_wdata1 = UInt(32.W)
    // val csr_wdata2 = UInt(32.W)
    val csr_wen1   = Bool()
    // val csr_wen2   = Bool()
    // val csr_wd_sel = Bool()
    // val csr_ws     = UInt(3.W)
    // val csr_imm    = UInt(32.W)
    // val csr_rdata  = UInt(32.W)
    // val snpc       = UInt(32.W)
    // val pc         = UInt(32.W)
    
    val dnpc = UInt(32.W)

    val is_brk     = Bool()
    val is_ivd     = Bool()
}

class WBUMessage extends Bundle {
    val dnpc = UInt(32.W)
    val pc_sel = Bool()
}
