package com.justnopoint.matsuri

import com.justnopoint.util.getShortAt
import okio.FileHandle
import okio.buffer

// Gatk chunks contain attack data.  This data is referenced directly by offset.
class GatkFile(val raf: FileHandle, node: Node) {
    val buffer = raf.source().buffer()
    val offset: Long

    init {
        raf.reposition(buffer, node.offset)
        val chunkSize = buffer.readIntLe()
        val prefix = buffer.readByteArray(buffer.readByte().toLong())
        offset = raf.position(buffer)
    }

    fun getHitdef(offset: Int): Hitdef {
        raf.reposition(buffer, this.offset + offset)
        val data = buffer.readByteArray(34)
        return Hitdef(data)
    }

    class Hitdef(val data: ByteArray) {
        fun isAirUnblockable(): Boolean {
            return (data[0].toInt() and 0x01) != 0
        }
        fun isHighUnblockable(): Boolean { // a.k.a is this a low
            return (data[0].toInt() and 0x02) != 0
        }
        fun isLowUnblockable(): Boolean { // a.k.a is this an overhead
            return (data[0].toInt() and 0x04) != 0
        }
        fun isKnockdown(): Boolean { // drains all red health
            return (data[1].toInt() and 0x08) != 0
        }
        fun getHitAnim(): Int { // Only two bits to define hit strength
            val flags = data[1].toInt()
            var anim = 147 + ((flags and 0x06) shr 1)
            anim += ((flags and 0x01) * 4) // Bent over animation starts at 151
            return anim
        }
        fun getDamage(): Float {
            return data.getShortAt(8)/2f
        }
        fun getGuardGaugeDamage(): Int { //High values instantly break guard
            return data.getShortAt(14)
        }
        fun getVelocityX(): Int {
            return data.getShortAt(16)
        }
        fun getHitPause(): Int { //Affects both players
            return data.getShortAt(18)
        }
        fun getHitShakeTime(): Int { //No effect on framedata
            return data.getShortAt(20)
        }
        fun getPower(): Int { //Opponent will gain a proportional amount
            return data.getShortAt(24)
        }
    }
}