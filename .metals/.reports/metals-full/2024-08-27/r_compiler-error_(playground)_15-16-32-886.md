file://<WORKSPACE>/npc/ssrc/cpu/lsu/lsu.scala
### java.lang.IndexOutOfBoundsException: -1 is out of bounds (min 0, max 2)

occurred in the presentation compiler.

presentation compiler configuration:
Scala version: 2.13.14
Classpath:
<WORKSPACE>/npc/.bloop/out/playground/bloop-bsp-clients-classes/classes-Metals-B64ESNPvRMy3gdw7otcEfA== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.10.0/semanticdb-javac-0.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/chipsalliance/chisel_2.13/6.4.0/chisel_2.13-6.4.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/scala-lang/scala-library/2.13.14/scala-library-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.14/scala-reflect-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/github/scopt/scopt_2.13/4.1.0/scopt_2.13-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/net/jcazevedo/moultingyaml_2.13/0.4.2/moultingyaml_2.13-0.4.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/json4s/json4s-native_2.13/4.0.6/json4s-native_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/apache/commons/commons-text/1.10.0/commons-text-1.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/io/github/alexarchambault/data-class_2.13/0.2.6/data-class_2.13-0.2.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/lihaoyi/os-lib_2.13/0.9.2/os-lib_2.13-0.9.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/scala-lang/modules/scala-parallel-collections_2.13/1.0.4/scala-parallel-collections_2.13-1.0.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/lihaoyi/upickle_2.13/3.1.0/upickle_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/chipsalliance/firtool-resolver_2.13/1.3.0/firtool-resolver_2.13-1.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/github/nscala-time/nscala-time_2.13/2.22.0/nscala-time_2.13-2.22.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/yaml/snakeyaml/1.26/snakeyaml-1.26.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/json4s/json4s-core_2.13/4.0.6/json4s-core_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/json4s/json4s-native-core_2.13/4.0.6/json4s-native-core_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/lihaoyi/geny_2.13/1.0.0/geny_2.13-1.0.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/lihaoyi/ujson_2.13/3.1.0/ujson_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/lihaoyi/upack_2.13/3.1.0/upack_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/lihaoyi/upickle-implicits_2.13/3.1.0/upickle-implicits_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/dev/dirs/directories/26/directories-26.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/outr/scribe_2.13/3.13.0/scribe_2.13-3.13.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/io/get-coursier/coursier_2.13/2.1.8/coursier_2.13-2.1.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/joda-time/joda-time/2.10.1/joda-time-2.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/joda/joda-convert/2.2.0/joda-convert-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/json4s/json4s-ast_2.13/4.0.6/json4s-ast_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/json4s/json4s-scalap_2.13/4.0.6/json4s-scalap_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/thoughtworks/paranamer/paranamer/2.8/paranamer-2.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/lihaoyi/upickle-core_2.13/3.1.0/upickle-core_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/outr/perfolation_2.13/1.2.9/perfolation_2.13-1.2.9.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/lihaoyi/sourcecode_2.13/0.3.1/sourcecode_2.13-0.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/scala-lang/modules/scala-collection-compat_2.13/2.11.0/scala-collection-compat_2.13-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/outr/moduload_2.13/1.1.7/moduload_2.13-1.1.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/github/plokhotnyuk/jsoniter-scala/jsoniter-scala-core_2.13/2.13.5.2/jsoniter-scala-core_2.13-2.13.5.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/io/get-coursier/coursier-core_2.13/2.1.8/coursier-core_2.13-2.1.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/io/get-coursier/coursier-cache_2.13/2.1.8/coursier-cache_2.13-2.1.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/io/get-coursier/coursier-proxy-setup/2.1.8/coursier-proxy-setup-2.1.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/io/github/alexarchambault/concurrent-reference-hash-map/1.1.0/concurrent-reference-hash-map-1.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/scala-lang/modules/scala-xml_2.13/2.2.0/scala-xml_2.13-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/io/get-coursier/coursier-util_2.13/2.1.8/coursier-util_2.13-2.1.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/io/get-coursier/jniutils/windows-jni-utils/0.3.3/windows-jni-utils-0.3.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/codehaus/plexus/plexus-archiver/4.9.0/plexus-archiver-4.9.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/codehaus/plexus/plexus-container-default/2.1.1/plexus-container-default-2.1.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/virtuslab/scala-cli/config_2.13/0.2.1/config_2.13-0.2.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/io/github/alexarchambault/windows-ansi/windows-ansi/0.0.5/windows-ansi-0.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/javax/inject/javax.inject/1/javax.inject-1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/codehaus/plexus/plexus-utils/4.0.0/plexus-utils-4.0.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/codehaus/plexus/plexus-io/3.4.1/plexus-io-3.4.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/commons-io/commons-io/2.15.0/commons-io-2.15.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/apache/commons/commons-compress/1.24.0/commons-compress-1.24.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/iq80/snappy/snappy/0.4/snappy-0.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/tukaani/xz/1.9/xz-1.9.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/com/github/luben/zstd-jni/1.5.5-10/zstd-jni-1.5.5-10.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/codehaus/plexus/plexus-classworlds/2.6.0/plexus-classworlds-2.6.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/apache/xbean/xbean-reflect/3.7/xbean-reflect-3.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo.scala-sbt.org/scalasbt/maven-releases/org/fusesource/jansi/jansi/1.18/jansi-1.18.jar [exists ]
Options:
-language:reflectiveCalls -deprecation -feature -Xcheckinit -Yrangepos -Xplugin-require:semanticdb


action parameters:
uri: file://<WORKSPACE>/npc/ssrc/cpu/lsu/lsu.scala
text:
```scala
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
    val addr = io.in.bits.exu_result
    val offset = addr(1,0)
    val mem_type = io.in.bits.mem_type
    
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
    val wmask = MuxLookup(mem_type, 0.U) (Seq(
        MemType.B  -> wmask_b,
        MemType.BU -> wmask_b,
        MemType.H  -> wmask_h,
        MemType.HU -> wmask_h,
        MemType.W  -> wmask_w
    ))
    // val wmask = Mux(mem_type(1), wmask_w, Mux(mem_type(0), wmask_h, wmask_b))

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
    size := Cat(0.B, mem_type(1, 0))

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

    val origin_rdata_0 = io.mem.rdata( 7,  0)
    val origin_rdata_1 = io.mem.rdata(15,  8)
    val origin_rdata_2 = io.mem.rdata(23, 16)
    val origin_rdata_3 = io.mem.rdata(31, 24)
    val mem_rdata_sign = MuxLookup(offset, 0.U)(Seq(
        0.U -> Mux(Mux(mem_type(0), io.mem.rdata(15), io.mem.rdata( 7)), 255.U(8.W), 0.U(8.W)),
        1.U -> Mux(io.mem.rdata(15), 255.U(8.W), 0.U(8.W)),
        2.U -> Mux(Mux(mem_type(0), io.mem.rdata(31), io.mem.rdata(23)), 255.U(8.W), 0.U(8.W)),
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
    val mem_rdata_1 = Mux(mem_type(1), origin_rdata_1, Mux(mem_type(0), mem_rdata_1_h, Mux(mem_type(2), 0.U(8.W), mem_rdata_sign)))
    // val mem_rdata_2 = MuxLookup(mem_type, 0.U)(Seq(
    //     MemType.B  -> mem_rdata_sign,
    //     MemType.BU -> 0.U(8.W),
    //     MemType.H  -> mem_rdata_sign,
    //     MemType.HU -> 0.U(8.W),
    //     MemType.W  -> io.mem.rdata(23, 16)
    // ))
    val mem_rdata_2 = Mux(mem_type(1), origin_rdata_2, Mux(mem_type(2), 0.U(8.W), mem_rdata_sign))
    val mem_rdata_3 = Mux(mem_type(1), origin_rdata_3, Mux(mem_type(2), 0.U(8.W), mem_rdata_sign))
    
    val mem_rdata = RegInit(0.U(32.W))
    mem_rdata := Mux(state === s_wait_mem && mem_ren, Cat(mem_rdata_3, mem_rdata_2, mem_rdata_1, mem_rdata_0), mem_rdata)
    
    io.out.bits.gpr_wdata :=  MuxLookup(io.in.bits.reg_ws, 0.U)(Seq (
        GPRWSel.EXU.id.U -> io.in.bits.exu_result,
        GPRWSel. SN.id.U -> io.in.bits.exu_result,
        GPRWSel.MEM.id.U -> mem_rdata,
        GPRWSel. SN.id.
        GPRWSel.CSR.id.U -> io.in.bits.csr_rdata
    ))

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
    io.out.bits.reg_wen := io.in.bits.reg_wen
    io.out.bits.reg_ws  := io.in.bits.reg_ws

    io.out.bits.csr_waddr1 := io.in.bits.csr_waddr1
    io.out.bits.is_ecall   := io.in.bits.is_ecall
    // io.out.bits.csr_waddr2 := io.in.bits.csr_waddr2
    io.out.bits.csr_wdata1 := io.in.bits.csr_wdata1
    io.out.bits.csr_wdata2 := io.in.bits.csr_wdata2
    io.out.bits.csr_wen1   := io.in.bits.csr_wen1
    // io.out.bits.csr_wen2   := io.in.bits.csr_wen2
    // io.out.bits.csr_wd_sel := io.in.bits.csr_wd_sel
    io.out.bits.csr_ws     := io.in.bits.csr_ws
    // io.out.bits.pc         := io.in.bits.pc
    
    io.out.bits.dnpc := io.in.bits.dnpc

    io.out.bits.is_brk := io.in.bits.is_brk
    io.out.bits.is_ivd := io.in.bits.is_ivd
}

```



#### Error stacktrace:

```
scala.collection.mutable.ArrayBuffer.apply(ArrayBuffer.scala:102)
	scala.reflect.internal.Types$Type.findMemberInternal$1(Types.scala:1030)
	scala.reflect.internal.Types$Type.findMember(Types.scala:1035)
	scala.reflect.internal.Types$Type.memberBasedOnName(Types.scala:661)
	scala.reflect.internal.Types$Type.member(Types.scala:625)
	scala.tools.nsc.typechecker.ContextErrors$TyperContextErrors$TyperErrorGen$.addendum$2(ContextErrors.scala:494)
	scala.tools.nsc.typechecker.ContextErrors$TyperContextErrors$TyperErrorGen$.errMsg$1(ContextErrors.scala:516)
	scala.tools.nsc.typechecker.ContextErrors$TyperContextErrors$TyperErrorGen$.NotAMemberError(ContextErrors.scala:519)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typed1$1(Typers.scala:4691)
	scala.tools.nsc.typechecker.Typers$Typer.lookupInQualifier$1(Typers.scala:4690)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typed1$62(Typers.scala:5506)
	scala.tools.nsc.typechecker.Typers$Typer.handleMissing$1(Typers.scala:5506)
	scala.tools.nsc.typechecker.Typers$Typer.typedSelect$1(Typers.scala:5511)
	scala.tools.nsc.typechecker.Typers$Typer.typedSelectOrSuperCall$1(Typers.scala:5604)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6206)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Typers$Typer.typedSelect$1(Typers.scala:5457)
	scala.tools.nsc.typechecker.Typers$Typer.typedSelectOrSuperCall$1(Typers.scala:5604)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6206)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Typers$Typer.typedArg(Typers.scala:3557)
	scala.tools.nsc.typechecker.Typers$Typer.typedArg0$1(Typers.scala:3665)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$doTypedApply$7(Typers.scala:3684)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$doTypedApply$6(Typers.scala:3663)
	scala.tools.nsc.typechecker.Contexts$Context.savingUndeterminedTypeParams(Contexts.scala:546)
	scala.tools.nsc.typechecker.Typers$Typer.handleOverloaded$1(Typers.scala:3660)
	scala.tools.nsc.typechecker.Typers$Typer.doTypedApply(Typers.scala:3712)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typed1$28(Typers.scala:5166)
	scala.tools.nsc.typechecker.Typers$Typer.silent(Typers.scala:727)
	scala.tools.nsc.typechecker.Typers$Typer.tryTypedApply$1(Typers.scala:5166)
	scala.tools.nsc.typechecker.Typers$Typer.normalTypedApply$1(Typers.scala:5254)
	scala.tools.nsc.typechecker.Typers$Typer.typedApply$1(Typers.scala:5267)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6205)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Typers$Typer.typedArg(Typers.scala:3557)
	scala.tools.nsc.typechecker.Typers$Typer.handlePolymorphicCall$1(Typers.scala:3961)
	scala.tools.nsc.typechecker.Typers$Typer.doTypedApply(Typers.scala:3980)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typed1$28(Typers.scala:5166)
	scala.tools.nsc.typechecker.Typers$Typer.silent(Typers.scala:713)
	scala.tools.nsc.typechecker.Typers$Typer.tryTypedApply$1(Typers.scala:5166)
	scala.tools.nsc.typechecker.Typers$Typer.normalTypedApply$1(Typers.scala:5254)
	scala.tools.nsc.typechecker.Typers$Typer.typedApply$1(Typers.scala:5267)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6205)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Typers$Typer.typedArg(Typers.scala:3557)
	scala.tools.nsc.typechecker.Typers$Typer.handlePolymorphicCall$1(Typers.scala:3961)
	scala.tools.nsc.typechecker.Typers$Typer.doTypedApply(Typers.scala:3980)
	scala.tools.nsc.typechecker.Typers$Typer.normalTypedApply$1(Typers.scala:5256)
	scala.tools.nsc.typechecker.Typers$Typer.typedApply$1(Typers.scala:5267)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6205)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Typers$Typer.typedArg(Typers.scala:3557)
	scala.tools.nsc.typechecker.Typers$Typer.handlePolymorphicCall$1(Typers.scala:3961)
	scala.tools.nsc.typechecker.Typers$Typer.doTypedApply(Typers.scala:3980)
	scala.tools.nsc.typechecker.Typers$Typer.normalTypedApply$1(Typers.scala:5256)
	scala.tools.nsc.typechecker.Typers$Typer.typedApply$1(Typers.scala:5267)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6205)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Typers$Typer.typedArg(Typers.scala:3557)
	scala.tools.nsc.typechecker.PatternTypers$PatternTyper.typedArgWithFormal$1(PatternTypers.scala:136)
	scala.tools.nsc.typechecker.PatternTypers$PatternTyper.$anonfun$typedArgsForFormals$4(PatternTypers.scala:150)
	scala.tools.nsc.typechecker.PatternTypers$PatternTyper.typedArgsForFormals(PatternTypers.scala:150)
	scala.tools.nsc.typechecker.PatternTypers$PatternTyper.typedArgsForFormals$(PatternTypers.scala:131)
	scala.tools.nsc.typechecker.Typers$Typer.typedArgsForFormals(Typers.scala:203)
	scala.tools.nsc.typechecker.Typers$Typer.handleMonomorphicCall$1(Typers.scala:3892)
	scala.tools.nsc.typechecker.Typers$Typer.doTypedApply(Typers.scala:3943)
	scala.tools.nsc.typechecker.Typers$Typer.normalTypedApply$1(Typers.scala:5256)
	scala.tools.nsc.typechecker.Typers$Typer.typedApply$1(Typers.scala:5267)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6205)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Typers$Typer.computeType(Typers.scala:6350)
	scala.tools.nsc.typechecker.Namers$Namer.assignTypeToTree(Namers.scala:1105)
	scala.tools.nsc.typechecker.Namers$Namer.inferredValTpt$1(Namers.scala:1752)
	scala.tools.nsc.typechecker.Namers$Namer.valDefSig(Namers.scala:1765)
	scala.tools.nsc.typechecker.Namers$Namer.memberSig(Namers.scala:1953)
	scala.tools.nsc.typechecker.Namers$Namer.typeSig(Namers.scala:1903)
	scala.tools.nsc.typechecker.Namers$Namer$ValTypeCompleter.completeImpl(Namers.scala:918)
	scala.tools.nsc.typechecker.Namers$LockingTypeCompleter.complete(Namers.scala:2100)
	scala.tools.nsc.typechecker.Namers$LockingTypeCompleter.complete$(Namers.scala:2098)
	scala.tools.nsc.typechecker.Namers$TypeCompleterBase.complete(Namers.scala:2093)
	scala.reflect.internal.Symbols$Symbol.completeInfo(Symbols.scala:1566)
	scala.reflect.internal.Symbols$Symbol.info(Symbols.scala:1538)
	scala.reflect.internal.Symbols$Symbol.initialize(Symbols.scala:1733)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:5835)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Typers$Typer.typedStat$1(Typers.scala:6339)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typedStats$9(Typers.scala:3539)
	scala.tools.nsc.typechecker.Typers$Typer.typedStats(Typers.scala:3539)
	scala.tools.nsc.typechecker.Typers$Typer.typedTemplate(Typers.scala:2144)
	scala.tools.nsc.typechecker.Typers$Typer.typedClassDef(Typers.scala:1982)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6168)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Typers$Typer.typedStat$1(Typers.scala:6339)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typedStats$9(Typers.scala:3539)
	scala.tools.nsc.typechecker.Typers$Typer.typedStats(Typers.scala:3539)
	scala.tools.nsc.typechecker.Typers$Typer.typedPackageDef$1(Typers.scala:5844)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6171)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Analyzer$typerFactory$TyperPhase.apply(Analyzer.scala:125)
	scala.tools.nsc.Global$GlobalPhase.applyPhase(Global.scala:481)
	scala.tools.nsc.interactive.Global$TyperRun.applyPhase(Global.scala:1369)
	scala.tools.nsc.interactive.Global$TyperRun.typeCheck(Global.scala:1362)
	scala.tools.nsc.interactive.Global.typeCheck(Global.scala:680)
	scala.meta.internal.pc.Compat.$anonfun$runOutline$1(Compat.scala:57)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:619)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:617)
	scala.collection.AbstractIterable.foreach(Iterable.scala:935)
	scala.meta.internal.pc.Compat.runOutline(Compat.scala:49)
	scala.meta.internal.pc.Compat.runOutline(Compat.scala:35)
	scala.meta.internal.pc.Compat.runOutline$(Compat.scala:33)
	scala.meta.internal.pc.MetalsGlobal.runOutline(MetalsGlobal.scala:36)
	scala.meta.internal.pc.ScalaCompilerWrapper.compiler(ScalaCompilerAccess.scala:19)
	scala.meta.internal.pc.ScalaCompilerWrapper.compiler(ScalaCompilerAccess.scala:14)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$semanticTokens$1(ScalaPresentationCompiler.scala:185)
```
#### Short summary: 

java.lang.IndexOutOfBoundsException: -1 is out of bounds (min 0, max 2)