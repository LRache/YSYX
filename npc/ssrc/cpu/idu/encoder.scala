package cpu.idu

import scala.collection.mutable.Map
import chisel3._
import chisel3.util.BitPat

class BitPatEncoder {
    private class Tag(s: Int, l: Int, i: Int) {
        val start = s
        val length = l
        val mask = (1 << l) - 1
        val index = i
    }

    private val tags: Map[String, Tag] = Map()
    private var current = 0
    private var count = 0

    def add_tag(name: String, length: Int) = {
        tags += (name -> new Tag(current, length, count))
        current += length
        count += 1
    }

    def get_tag(name: String, bits: UInt) : UInt = {
        val tag = tags(name)
        return bits(tag.start + tag.length - 1, tag.start)
    }

    def gen_list(attr: Map[String, Int]) : List[UInt] = {
        var l: List[UInt] = List.fill(count)(UInt())
        for ((name, tag) <- tags) {
            l(tag.index) := attr(name).U
        }
        return l
    }

    def gen_bitpat(attr: Map[String, Int]) : BitPat = {
        var bits: Long = 0L
        for ((name, tag) <- tags) {
            bits |= (attr(name) & tag.mask).toLong << tag.start
        }
        return BitPat(bits.U(current.W))
    }
}
