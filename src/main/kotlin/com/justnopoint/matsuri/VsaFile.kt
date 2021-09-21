package com.justnopoint.matsuri

import okio.FileHandle
import okio.buffer

class VsaFile(val handle: FileHandle) {
    val chunks = HashMap<String, Node>()

    init {
        val buffer = handle.source().buffer()
        val check = ByteArray(8)
        buffer.read(check)
        if(String(check) != VSA) {
            error("Bad header.")
        } else {
            var tag = ""
            val tagbytes = ByteArray(4)
            while(tag != END) {
                val size = buffer.readIntLe()
                buffer.read(tagbytes)
                val extra = buffer.readShortLe().toInt()
                tag = String(tagbytes)
                if(size != 0) {
                    chunks[tag] = Node(handle.position(buffer), size, extra)
                    buffer.skip(size.toLong())
                }
            }
        }
    }

    fun getNode(tag: String): Node? {
        return chunks[tag]
    }

    companion object {
        val VSA = String(byteArrayOf(0x76, 0x73, 0x61, 0x20, 0x00, 0x00, 0x01, 0x00))
        val END = String(byteArrayOf(0x65, 0x6e, 0x64, 0x20))
        const val IMAG = "imag"
        const val GRAP = "grap"
        const val ANIM = "anim"
        const val GANM = "ganm"
        const val GREC = "grec"
        const val THWP = "thwp"
        const val GATK = "gatk"
        const val PPAL = "ppal"
        const val PIDX = "pidx"
    }
}

data class Node(val offset: Long, val size: Int, val extra: Int)