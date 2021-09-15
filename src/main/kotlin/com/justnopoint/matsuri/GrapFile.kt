package com.justnopoint.matsuri

import ar.com.hjg.pngj.ImageLineInt
import ar.com.hjg.pngj.ImageLineSetDefault
import com.justnopoint.util.readIntLe
import com.justnopoint.util.readShortLe
import java.awt.image.BufferedImage
import java.io.RandomAccessFile

class GrapFile(private val raf: RandomAccessFile, node: Node, private val sheets: ImagFile) {
    val names = ArrayList<String>(node.extra)
    val offsets = ArrayList<Long>(node.extra)

    init {
        raf.seek(node.offset)

        for(n in 0 until node.extra) {
            val chunkSize = raf.readIntLe()
            val name = ByteArray(raf.readUnsignedByte())
            raf.read(name)
            names.add(n, String(name))
            offsets.add(n, raf.filePointer)
            raf.skipBytes(chunkSize)
        }
    }

    private fun getSpriteDimensions(index: Int): Pair<Int, Int> {
        raf.seek(offsets[index])

        val sections = raf.readIntLe()
        val three = raf.readByte()
        val ox = raf.readShortLe()
        val oy = raf.readShortLe()
        val ow = raf.readShortLe()
        val oh = raf.readShortLe()
        return Pair(ox+ow, oy+oh)
    }

    private fun buildSprite(index: Int): BufferedImage {
        raf.seek(offsets[index])

        val sections = raf.readIntLe()
        val three = raf.readByte()
        val ox = raf.readShortLe()
        val oy = raf.readShortLe()
        val ow = raf.readShortLe()
        val oh = raf.readShortLe()
        val currentSprite = BufferedImage(ox+ow, oy+oh, BufferedImage.TYPE_INT_ARGB)
        val raster = currentSprite.raster
        var currentSheet: ImageLineSetDefault<ImageLineInt>? = null
        var sheetMode = raf.readUnsignedByte()
        var name: ByteArray
        while(sheetMode != 0) {
            when(sheetMode) {
                1 -> {
                    name = ByteArray(raf.readUnsignedByte())
                    raf.read(name)
                    currentSheet = sheets.getSheet(String(name))
                }
                2 -> {
                    val dx = raf.readShortLe()
                    val dy = raf.readShortLe()
                    val sx = raf.readShortLe()
                    val sy = raf.readShortLe()
                    val sw = raf.readShortLe()
                    val sh = raf.readShortLe()
                    currentSheet?.getRect(sx, sy, sw, sh)?.forEachIndexed { y, line ->
                        raster.setPixels(dx, dy+y, sw, 1, line)
                    }
                }
                else -> {
                    error("Unhandled mode byte. $sheetMode")
                }
            }
            sheetMode = raf.readUnsignedByte()
        }

        return currentSprite
    }

    fun getSpriteDimensions(name: String?): Pair<Int, Int> {
        if(name == null) {
            return Pair(0,0)
        }
        if(!names.contains(name)) {
            return Pair(0,0)
        }
        return getSpriteDimensions(names.indexOf(name))
    }

    fun getSprite(name: String?): BufferedImage? {
        if(name == null) {
            return null
        }
        if(!names.contains(name)) {
            //println("Sprite $name not found.")
            return null
        }
        return buildSprite(names.indexOf(name))
    }

    companion object {
        const val CHANNELS = 4
    }
}