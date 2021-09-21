package com.justnopoint.matsuri

import okio.FileHandle
import okio.buffer

// Grec chunks contain rectangle data for hitboxes.
class GrecFile(raf: FileHandle, node: Node) {
    val buffer = raf.source().buffer()
    val boxes: List<GrecBoxes>

    init {
        raf.reposition(buffer, node.offset)
        val offsets = ArrayList<Long>()
        val chunksize = buffer.readIntLe()
        val prefix = buffer.readByteArray(buffer.readByte().toLong())
        val startOff = raf.position(buffer)
        offsets.add(buffer.readIntLe()+startOff)
        while(raf.position(buffer) < offsets[0]) {
            offsets.add(buffer.readIntLe()+startOff)
        }
        val boxTypes = ByteArray(4)
        boxes = offsets.map { offset ->
            raf.reposition(buffer, offset)
            buffer.read(boxTypes)
            boxTypes.map{ count ->
                (0 until count).map {
                    GrecBox(buffer.readShortLe().toInt(), buffer.readShortLe().toInt(),
                        buffer.readShortLe().toInt(), buffer.readShortLe().toInt()
                    )
                }
            }
        }.map { n -> GrecBoxes(n) }

        for(grecbox in boxes) {
            if(grecbox.boxtype.size != 4) {
                println("Empty lists not generated!")
            }
        }
    }

    fun getBoxes(index: Int): GrecBoxes? {
        if(index < 0) {
            return null
        }
        if(index >= boxes.size) {
            return null
        }
        return boxes[index]
    }
}

data class GrecBoxes(val boxtype: List<List<GrecBox>>)
data class GrecBox(val x: Int, val y: Int, val width: Int, val height: Int)