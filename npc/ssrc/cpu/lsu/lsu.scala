package cpu.lsu

import chisel3._
import chisel3.util._

import cpu.EXUMessage
import cpu.LSUMessage

import bus.AXI4IO

object MemType extends Enumeration {
    type MemType = Value
    val B, BU, H, HU, W, N = Value
}

class LSU extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new EXUMessage))
        val out = Decoupled(new LSUMessage)

        val mem = new AXI4IO
    })
    val addr = io.in.bits.exu_result
    val offset = (addr & 0x3.U)(1,0)
    val mem_type = io.in.bits.mem_type
    
    val wmask = MuxCase(0.U, Seq(
        (mem_type === MemType.B.id.U || mem_type === MemType.BU.id.U) -> MuxLookup(offset, 0.U)(Seq (
            0.U -> 0b0001.U,
            1.U -> 0b0010.U,
            2.U -> 0b0100.U,
            3.U -> 0b1000.U
        )),
        (mem_type === MemType.H.id.U || mem_type === MemType.HU.id.U) -> MuxLookup(offset, 0.U)(Seq (
            0.U -> 0b0011.U,
            2.U -> 0b1100.U
        )),
        (mem_type === MemType.W.id.U) -> 0b1111.U
    ))

    val rs2 = io.in.bits.rs2
    val wdata = MuxLookup(wmask, rs2)(Seq (
        0b0001.U -> Cat(0.U(24.W), rs2( 7, 0)           ),
        0b0010.U -> Cat(0.U(16.W), rs2( 7, 0), 0.U( 8.W)),
        0b0100.U -> Cat(0.U( 8.W), rs2( 7, 0), 0.U(16.W)),
        0b1000.U -> Cat(           rs2( 7, 0), 0.U(24.W)),
        0b0011.U -> Cat(0.U(16.W), rs2(15, 0)           ),
        0b1100.U -> Cat(           rs2(15, 0), 0.U(16.W)),
        0b1111.U -> rs2
    ))

    val size = Wire(UInt(3.W))
    size := MuxLookup(mem_type, 0.U)(Seq (
        MemType. B.id.U -> 0.U,
        MemType.BU.id.U -> 0.U,
        MemType. H.id.U -> 1.U,
        MemType.HU.id.U -> 1.U,
        MemType. W.id.U -> 2.U
    ))

    val s_wait_rv :: s_wait_mem :: s_valid :: s_error :: Nil = Enum(4)
    val state = RegInit(s_wait_rv)
    // when (io.in.valid) {
    //     when (io.in.bits.mem_ren) {
    //         state := MuxLookup(state, s_wait_rv) (Seq (
    //                 s_wait_rv   -> Mux(io.mem.arready, s_wait_mem, s_wait_rv),
    //                 s_wait_mem -> Mux(io.mem. rvalid, s_valid, s_wait_mem),
    //                 s_valid     -> s_wait_rv
    //             ))
    //     } .elsewhen(io.in.bits.mem_wen) {
    //         state := MuxLookup(state, s_valid) (Seq (
    //                 s_wait_rv   -> Mux(io.mem.awready && io.mem.wready, s_wait_mem, s_wait_rv),
    //                 s_wait_mem  -> Mux(io.mem.bvalid, s_valid, s_wait_mem),
    //                 s_valid     -> s_wait_rv
    //             ))
    //         // printf("%x\n", state)
    //         // assert(state === s_valid)
    //     } .otherwise {
    //         state := Mux(state === s_valid, s_wait_rv, s_valid)
    //     }
    // }
    // .otherwise {
    //     state := s_wait_rv
    // }
    state := Mux(
        io.in.valid,
        Mux(
            io.in.bits.mem_ren,
            MuxLookup(state, s_wait_rv) (Seq (
                s_wait_rv   -> Mux(io.mem.arready, s_wait_mem, s_wait_rv),
                s_wait_mem -> Mux(io.mem. rvalid, s_valid, s_wait_mem),
                s_valid     -> s_wait_rv
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

    val mem_wen = io.in.valid && io.in.bits.mem_wen
    val w_valid = mem_wen && state === s_wait_rv
    io.mem.awaddr  := addr
    io.mem.awvalid := w_valid
    io.mem. wdata  := wdata
    io.mem. wvalid := w_valid
    io.mem. wstrb  := wmask
    io.mem.awsize  := size
    io.mem.bready := state === s_wait_mem
    
    val mem_ren = io.in.valid && io.in.bits.mem_ren
    io.mem.araddr  := addr
    io.mem.arvalid := mem_ren && state === s_wait_rv
    io.mem.arsize  := size
    io.mem.rready  := mem_ren && state === s_wait_mem

    val mem_rdata = RegInit(0.U(32.W))
    mem_rdata := Mux(
        state === s_wait_mem && mem_ren,
        MuxLookup(mem_type, 0.U)(Seq (
            MemType. B.id.U -> MuxLookup(offset, 0.U)(Seq (
                0.U -> Cat(Fill(24, io.mem.rdata( 7)), io.mem.rdata( 7,  0)),
                1.U -> Cat(Fill(24, io.mem.rdata(15)), io.mem.rdata(15,  8)),
                2.U -> Cat(Fill(24, io.mem.rdata(23)), io.mem.rdata(23, 16)),
                3.U -> Cat(Fill(24, io.mem.rdata(31)), io.mem.rdata(31, 24)),
            )),
            MemType. H.id.U -> MuxLookup(offset, 0.U)(Seq (
                0.U -> Cat(Fill(16, io.mem.rdata(15)), io.mem.rdata(15,  0)),
                2.U -> Cat(Fill(16, io.mem.rdata(31)), io.mem.rdata(31, 16)),
            )),
            MemType.BU.id.U -> MuxLookup(offset, 0.U)(Seq (
                0.U -> Cat(0.U(24.W), io.mem.rdata( 7,  0)),
                1.U -> Cat(0.U(24.W), io.mem.rdata(15,  8)),
                2.U -> Cat(0.U(24.W), io.mem.rdata(23, 16)),
                3.U -> Cat(0.U(24.W), io.mem.rdata(31, 24)),
            )),
            MemType.HU.id.U -> MuxLookup(offset, 0.U)(Seq (
                0.U -> Cat(0.U(16.W), io.mem.rdata(15,  0)),
                2.U -> Cat(0.U(16.W), io.mem.rdata(31, 16)),
            )),
            MemType. W.id.U -> io.mem.rdata
        )),
        mem_rdata)
    io.out.bits.mem_rdata := mem_rdata

    io. in.ready := io.out.ready
    io.out.valid := state === s_valid

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
    io.out.bits.exu_result := io.in.bits.exu_result
        
    io.out.bits.rd      := io.in.bits.rd
    io.out.bits.reg_wen := io.in.bits.reg_wen
    io.out.bits.reg_ws  := io.in.bits.reg_ws

    io.out.bits.csr_waddr1 := io.in.bits.csr_waddr1
    io.out.bits.csr_waddr2 := io.in.bits.csr_waddr2
    io.out.bits.csr_wdata1 := io.in.bits.csr_wdata1
    io.out.bits.csr_wdata2 := io.in.bits.csr_wdata2
    io.out.bits.csr_wen1   := io.in.bits.csr_wen1
    io.out.bits.csr_wen2   := io.in.bits.csr_wen2
    io.out.bits.csr_wd_sel := io.in.bits.csr_wd_sel
    io.out.bits.csr_ws     := io.in.bits.csr_ws
    io.out.bits.csr_imm    := io.in.bits.csr_imm
    io.out.bits.csr_rdata  := io.in.bits.csr_rdata
    io.out.bits.snpc       := io.in.bits.snpc
    io.out.bits.pc         := io.in.bits.pc
    
    io.out.bits.dnpc := io.in.bits.dnpc

    io.out.bits.is_brk := io.in.bits.is_brk
    io.out.bits.is_ivd := io.in.bits.is_ivd
}
