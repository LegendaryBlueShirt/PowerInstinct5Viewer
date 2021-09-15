package com.justnopoint.matsuri

import com.justnopoint.util.readIntLe
import com.justnopoint.util.getShortAt
import java.io.RandomAccessFile

class GatkFile(val raf: RandomAccessFile, node: Node) {
    val offset: Long

    init {
        raf.seek(node.offset)
        val chunkSize = raf.readIntLe()
        val prefix = ByteArray(raf.readUnsignedByte())
        raf.read(prefix)
        offset = raf.filePointer
    }

    fun getHitdef(offset: Int): Hitdef {
        raf.seek(this.offset + offset)
        val data = ByteArray(34)
        raf.read(data)
        return Hitdef(data)
    }

    class Hitdef(val data: ByteArray) {
        fun getDamage(): Float {
            return data.getShortAt(8)/2f
        }
        fun getHitPause(): Int {
            return data.getShortAt(18)
        }
        fun getHitAnim(): Int {
            val flags = data[1].toInt()
            var anim = 147 + ((flags and 6) shr 1)
            anim += ((flags and 1) * 4)
            return anim
        }
    }
}