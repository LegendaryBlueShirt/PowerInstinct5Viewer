package com.justnopoint.matsuri

import com.justnopoint.util.readIntLe
import com.justnopoint.util.readShortLe
import java.io.RandomAccessFile

class ThwpFile(raf: RandomAccessFile, node: Node) {
    val prefix: String
    val coords = ArrayList<List<Pair<Int, Int>>>()

    init {
        raf.seek(node.offset)

        val chunkSize = raf.readIntLe()
        val prefix = ByteArray(raf.readUnsignedByte())
        raf.read(prefix)
        this.prefix = String(prefix)

        val start = raf.filePointer
        val offsets = ArrayList<Long>()
        offsets.add(raf.readIntLe()+start)
        while(raf.filePointer < offsets[0]) {
            offsets.add(raf.readIntLe()+start)
        }

        for(offset in offsets) {
            raf.seek(offset)
            val nCoords = raf.readShortLe()
            coords.add((0 until nCoords).map {
                Pair(raf.readShortLe(), raf.readShortLe())
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