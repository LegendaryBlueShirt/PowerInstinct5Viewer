package com.justnopoint.matsuri

import com.justnopoint.util.readIntLe
import com.justnopoint.util.readShortLe
import javafx.scene.shape.Rectangle
import java.io.RandomAccessFile

// Grec chunks contain rectangle data for hitboxes.
class GrecFile(raf: RandomAccessFile, node: Node) {
    val boxes: List<GrecBoxes>

    init {
        raf.seek(node.offset)
        val offsets = ArrayList<Long>()
        val chunksize = raf.readIntLe()
        val prefix = ByteArray(raf.readUnsignedByte())
        raf.read(prefix)
        val startOff = raf.filePointer
        offsets.add(raf.readIntLe()+startOff)
        while(raf.filePointer < offsets[0]) {
            offsets.add(raf.readIntLe()+startOff)
        }
        val boxTypes = ByteArray(4)
        boxes = offsets.map { offset ->
            raf.seek(offset)
            raf.read(boxTypes)
            boxTypes.map{ count ->
                (0 until count).map {
                    Rectangle(raf.readShortLe().toDouble(), raf.readShortLe().toDouble(),
                        raf.readShortLe().toDouble(), raf.readShortLe().toDouble()
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

data class GrecBoxes(val boxtype: List<List<Rectangle>>)