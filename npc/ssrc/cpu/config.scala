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
    val HasBTB = true
    
    // ysyx
    val VendorID = 0x79737938
    val ArchID = 0x24080016

    // IFU
    val PCWidth = 30
    
    // GPR
    val GPRAddrWidth = 4
    
    // CSR
    val CSRAddrWidth = 3
    val CSRInitValue = Map(
        "mvendorid" -> VendorID,
        "marchid"   -> ArchID,
        "satp"      -> 0,
        "mstatus"   -> 0x1800,
    )
}
