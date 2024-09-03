package cpu.lsu

import chisel3._
import chisel3.util._

import cpu.EXUMessage
import cpu.LSUMessage

import bus.AXI4IO
import cpu.LSUPerfCounter
import cpu.reg.GPRWSel

object MemType{
    val B  = 0.U
    val H  = 1.U
    val W  = 2.U
    val BU = 4.U
    val HU = 5.U
}

class LSU extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new EXUMessage))
        val out = Decoupled(new LSUMessage)

        val mem = new AXI4IO
        val perf = new LSUPerfCounter
    })
    val s_wait_rv :: s_wait_mem :: s_valid :: s_error :: Nil = Enum(4)
    val state = RegInit(s_wait_rv)
    state := Mux(
        io.in.valid,
        Mux(
            io.in.bits.mem_ren,
            MuxLookup(state, s_wait_rv) (Seq (
                s_wait_rv  -> Mux(io.mem.arready, s_wait_mem, s_wait_rv),
                s_wait_mem -> Mux(io.mem. rvalid, s_valid, s_wait_mem),
                s_valid    -> s_wait_rv
            )),
            Mux(
                io.in.bits.mem_wen,
                MuxLookup(state, s_valid) (Seq (
                    s_wait_rv   -> Mux(io.mem.awready && io.mem.wready, s_wait_mem, s_wait_rv),
                    s_wait_mem  -> Mux(io.mem.bvalid, s_valid, s_wait_mem),
                    s_valid     -> s_wait_rv
                )),
                Mux(state === s_valid, s_wait_rv, s_valid)
            )
        ),
        s_wait_rv
    )

    // COMMON
    val addr = io.in.bits.exu_result
    val offset = addr(1,0)
    val memType = io.in.bits.mem_type
    val size = Cat(0.B, memType(1, 0))

    // WRITE
    // WMASK
    val wmask_b = MuxLookup(offset, 0.U)(Seq (
        0.U -> 0b0001.U,
        1.U -> 0b0010.U,
        2.U -> 0b0100.U,
        3.U -> 0b1000.U
    ))
    val wmask_h = MuxLookup(offset, 0.U)(Seq (
        0.U -> 0b0011.U,
        2.U -> 0b1100.U
    ))
    val wmask_w = 0b1111.U
    // val wmask = MuxLookup(mem_type, 0.U) (Seq(
    //     MemType.B  -> wmask_b,
    //     MemType.BU -> wmask_b,
    //     MemType.H  -> wmask_h,
    //     MemType.HU -> wmask_h,
    //     MemType.W  -> wmask_w
    // ))
    val wmask = Mux(memType(1), wmask_w, Mux(memType(0), wmask_h, wmask_b))

    // WDATA
    val rs = io.in.bits.rs2
    // val wdata = MuxLookup(wmask, rs)(Seq (
    //     0b0001.U -> Cat(0.U(24.W), rs( 7, 0)           ),
    //     0b0010.U -> Cat(0.U(16.W), rs( 7, 0), 0.U( 8.W)),
    //     0b0100.U -> Cat(0.U( 8.W), rs( 7, 0), 0.U(16.W)),
    //     0b1000.U -> Cat(           rs( 7, 0), 0.U(24.W)),
    //     0b0011.U -> Cat(0.U(16.W), rs(15, 0)           ),
    //     0b1100.U -> Cat(           rs(15, 0), 0.U(16.W)),
    //     0b1111.U -> rs
    // ))
    // val wdata = Mux(
    //     memType(1),
    //     // SW
    //     rs,
    //     Mux(
    //         memType(0),
    //         // SH
    //         Mux(
    //             offset(1), // offset ?= 2
    //             Cat(rs(15, 0), 0.U(16.W)),
    //             Cat(0.U(16.W), rs(15, 0))
    //         ),
    //         // SB
    //         MuxLookup(offset, 0.U)(Seq(
    //             0.U -> Cat(0.U(24.W), rs( 7, 0)           ),
    //             1.U -> Cat(0.U(16.W), rs( 7, 0), 0.U( 8.W)),
    //             2.U -> Cat(0.U( 8.W), rs( 7, 0), 0.U(16.W)),
    //             3.U -> Cat(           rs( 7, 0), 0.U(24.W)),
    //         ))
    //     )
    // )
    val wdata_b = Fill(4, rs( 7, 0))
    val wdata_h = Fill(2, rs(15, 0))
    val wdata_w = rs
    // val wdata = MuxLookup(memType, 0.U)(Seq(
    //     MemType.B -> wdata_b,
    //     MemType.H -> wdata_h,
    //     MemType.W -> wdata_w
    // ))
    val wdata = Mux(
        memType(1),
        wdata_w,
        Mux(
            memType(0),
            wdata_h,
            wdata_b
        )
    )
    // val rs_0 = rs( 7,  0)
    // val rs_1 = rs(15,  8)
    // val rs_2 = rs(23, 16)
    // val rs_3 = rs(31, 24)
    // val wdata_0 = rs_0
    // val wdata_1 = Mux(memType.orR, rs_1, rs_0)
    // val wdata_2 = Mux(memType(1), rs_2, rs_0)
    // val wdata_3 = Mux(memType(1), rs_3, Mux(memType(0), rs_1, rs_0))
    // val wdata = Cat(wdata_3, wdata_2, wdata_1, wdata_0)

    val mem_wen = io.in.valid && io.in.bits.mem_wen
    val w_valid = mem_wen && state === s_wait_rv
    io.mem.awaddr  := addr
    io.mem.awvalid := w_valid
    io.mem. wdata  := wdata
    io.mem. wvalid := w_valid
    io.mem. wstrb  := wmask
    io.mem.awsize  := size
    io.mem.bready := state === s_wait_mem

    // READ
    val mem_ren = io.in.valid && io.in.bits.mem_ren
    io.mem.araddr  := addr
    io.mem.arvalid := mem_ren && state === s_wait_rv
    io.mem.arsize  := size
    io.mem.rready  := mem_ren && state === s_wait_mem

    val origin_rdata_0 = io.mem.rdata( 7,  0)
    val origin_rdata_1 = io.mem.rdata(15,  8)
    val origin_rdata_2 = io.mem.rdata(23, 16)
    val origin_rdata_3 = io.mem.rdata(31, 24)
    val mem_rdata_sign = MuxLookup(offset, 0.U)(Seq(
        0.U -> Mux(Mux(memType(0), io.mem.rdata(15), io.mem.rdata( 7)), 255.U(8.W), 0.U(8.W)),
        1.U -> Mux(io.mem.rdata(15), 255.U(8.W), 0.U(8.W)),
        2.U -> Mux(Mux(memType(0), io.mem.rdata(31), io.mem.rdata(23)), 255.U(8.W), 0.U(8.W)),
        3.U -> Mux(io.mem.rdata(31), 255.U(8.W), 0.U(8.W)),
    ))
    val mem_rdata_0 = MuxLookup(offset, 0.U)(Seq(
        0.U -> origin_rdata_0,
        1.U -> origin_rdata_1,
        2.U -> origin_rdata_2,
        3.U -> origin_rdata_3
    ))
    val mem_rdata_1_h =MuxLookup(offset, 0.U)(Seq(
        0.U -> io.mem.rdata(15,  8),
        2.U -> io.mem.rdata(31, 24)
    ))
    // val mem_rdata_1_h = Mux(offset(1), io.mem.rdata(31, 24), io.mem.rdata(15, 8))
    // val mem_rdata_1 = MuxLookup(mem_type, 0.U)(Seq(
    //     MemType.B  -> mem_rdata_sign,
    //     MemType.BU -> 0.U(8.W),
    //     MemType.H  -> mem_rdata_1_h,
    //     MemType.HU -> mem_rdata_1_h,
    //     MemType.W  -> io.mem.rdata(15,8)
    // ))
    val mem_rdata_1 = Mux(memType(1), origin_rdata_1, Mux(memType(0), mem_rdata_1_h, Mux(memType(2), 0.U(8.W), mem_rdata_sign)))
    // val mem_rdata_2 = MuxLookup(mem_type, 0.U)(Seq(
    //     MemType.B  -> mem_rdata_sign,
    //     MemType.BU -> 0.U(8.W),
    //     MemType.H  -> mem_rdata_sign,
    //     MemType.HU -> 0.U(8.W),
    //     MemType.W  -> io.mem.rdata(23, 16)
    // ))
    val mem_rdata_2 = Mux(memType(1), origin_rdata_2, Mux(memType(2), 0.U(8.W), mem_rdata_sign))
    val mem_rdata_3 = Mux(memType(1), origin_rdata_3, Mux(memType(2), 0.U(8.W), mem_rdata_sign))
    
    val mem_rdata = RegInit(0.U(32.W))
    mem_rdata := Mux(state === s_wait_mem && mem_ren, Cat(mem_rdata_3, mem_rdata_2, mem_rdata_1, mem_rdata_0), mem_rdata)
    
    io.out.bits.gpr_wdata :=  MuxLookup(io.in.bits.gpr_ws, 0.U)(Seq (
        GPRWSel.EXU.id.U -> io.in.bits.exu_result,
        GPRWSel.MEM.id.U -> mem_rdata,
        GPRWSel. SN.id.U -> io.in.bits.snpc,
        GPRWSel.CSR.id.U -> io.in.bits.csr_rdata
    ))
    when (io.in.bits.gpr_wen) {
        printf("%d\n", io.in.bits.csr_rdata)
    }

    assert(io.mem.rresp === 0.U)

    io. in.ready := io.out.ready
    io.out.valid := state === s_valid

    io.perf.isWaiting := state === s_wait_mem
    io.perf.addr := addr
    io.perf.wen := io.in.bits.mem_wen
    io.perf.ren := io.in.bits.mem_ren

    // Unused
    io.mem.awid    := 0.U
    io.mem.awlen   := 0.U
    io.mem.awburst := 0.U
    io.mem.wlast   := true.B
    io.mem.arid    := 0.U
    io.mem.arlen   := 0.U
    io.mem.arburst := 0.U

    // Passthrough
    io.out.bits.pc_sel     := io.in.bits.pc_sel
        
    io.out.bits.rd      := io.in.bits.rd
    io.out.bits.gpr_wen := io.in.bits.gpr_wen
    // io.out.bits.reg_ws  := io.in.bits.reg_ws

    io.out.bits.csr_waddr1 := io.in.bits.csr_waddr1
    io.out.bits.is_ecall   := io.in.bits.is_ecall
    // io.out.bits.csr_waddr2 := io.in.bits.csr_waddr2
    io.out.bits.csr_wdata1 := io.in.bits.csr_wdata1
    // io.out.bits.csr_wdata2 := io.in.bits.csr_wdata2
    io.out.bits.csr_wen1   := io.in.bits.csr_wen1
    // io.out.bits.csr_wen2   := io.in.bits.csr_wen2
    // io.out.bits.csr_wd_sel := io.in.bits.csr_wd_sel
    // io.out.bits.csr_ws     := io.in.bits.csr_ws
    // io.out.bits.pc         := io.in.bits.pc
    
    io.out.bits.dnpc := io.in.bits.dnpc

    io.out.bits.is_brk := io.in.bits.is_brk
    io.out.bits.is_ivd := io.in.bits.is_ivd
}
