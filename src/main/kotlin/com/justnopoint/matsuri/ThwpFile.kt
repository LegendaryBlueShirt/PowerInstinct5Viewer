package com.justnopoint.matsuri

import okio.FileHandle
import okio.buffer

// Contains axis information for binding characters correctly during throws.
class ThwpFile(raf: FileHandle, node: Node) {
    val buffer = raf.source().buffer()
    val prefix: String
    val coords = ArrayList<List<Pair<Int, Int>>>()

    init {
        raf.reposition(buffer, node.offset)

        val chunkSize = buffer.readIntLe()
        val prefix = buffer.readByteArray(buffer.readByte().toLong())
        this.prefix = String(prefix)

        val start = raf.position(buffer)
        val offsets = ArrayList<Long>()
        offsets.add(buffer.readIntLe()+start)
        while(raf.position(buffer) < offsets[0]) {
            offsets.add(buffer.readIntLe()+start)
        }

        for(offset in offsets) {
            raf.reposition(buffer, offset)
            val nCoords = buffer.readShortLe()
            coords.add((0 until nCoords).map {
                Pair(buffer.readShortLe().toInt(), buffer.readShortLe().toInt())
            })
        }
    }

    fun getCoords(index: Int): List<Pair<Int,Int>> {
        if(index < 0) {
            return emptyList()
        }
        if(index >= coords.size) {
            return emptyList()
        }
        return coords[index]
    }
}