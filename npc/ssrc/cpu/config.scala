package cpu

import scala.collection.mutable.Map

object Config {
    val HasDBG = false

    // Config
    val HasMscratch = false
    val HasSatp = false
    val HasFastAlu = true
    val JudgeExuRaw = false

    val HasClint = true
    
    // ysyx
    val VendorID = 0x79737938
    val ArchID = 0x24080016
    
    // GPR
    val GPRAddrLength = 4
    val GPRInitValue = Seq.fill((1 << GPRAddrLength) - 1)(0)
    
    // CSR
    val CSRAddrLength = 3
    val CSRInitValue = Map(
        "mvendorid" -> VendorID,
        "marchid"   -> ArchID,
        "satp"      -> 0,
        "mstatus"   -> 0x1800,
        "mtvec"     -> 0,
        "mscratch"  -> 0,
        "mepc"      -> 0,
        "mcause"    -> 0
    )
}
