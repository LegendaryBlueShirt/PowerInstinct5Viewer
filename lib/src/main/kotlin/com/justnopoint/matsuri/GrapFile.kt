package com.justnopoint.matsuri

import com.justnopoint.interfaces.Tile
import com.justnopoint.interfaces.TiledImage
import okio.FileHandle
import okio.buffer

// For tiled images, Grap chunks contain the information to
// piece them together from sheets.
class GrapFile(private val raf: FileHandle, node: Node, val sheets: ImagFile) {
    val buffer = raf.source().buffer()
    val names = ArrayList<String>(node.extra)
    val offsets = ArrayList<Long>(node.extra)

    init {
        raf.reposition(buffer, node.offset)

        for(n in 0 until node.extra) {
            val chunkSize = buffer.readIntLe()
            val name = buffer.readByteArray(buffer.readByte().toLong())
            names.add(n, String(name))
            offsets.add(n, raf.position(buffer))
            buffer.skip(chunkSize.toLong())
        }
    }

    private fun getSpriteDimensions(index: Int): Pair<Int, Int> {
        raf.reposition(buffer, offsets[index])

        val sections = buffer.readIntLe()
        val three = buffer.readByte()
        val ox = buffer.readShortLe()
        val oy = buffer.readShortLe()
        val ow = buffer.readShortLe()
        val oh = buffer.readShortLe()
        return Pair(ox+ow, oy+oh)
    }

    private fun buildSprite(index: Int): TiledImage {
        raf.reposition(buffer, offsets[index])

        val sections = buffer.readIntLe()
        val three = buffer.readByte()
        val ox = buffer.readShortLe()
        val oy = buffer.readShortLe()
        val ow = buffer.readShortLe()
        val oh = buffer.readShortLe()
        val width = (ox+ow)
        val height = (oy+oh)
        val currentSprite = mutableListOf<Tile>()
        val coordinates = mutableListOf<Pair<Int, Int>>()
        lateinit var currentSheet: Any
        var sheetMode = buffer.readByte().toInt()
        var name: ByteArray
        while(sheetMode != 0) {
            when(sheetMode) {
                1 -> {
                    name = buffer.readByteArray(buffer.readByte().toLong())
                    currentSheet = sheets.getSheet(String(name))
                }
                2 -> {
                    val dx = buffer.readShortLe().toInt()
                    val dy = buffer.readShortLe().toInt()
                    val sx = buffer.readShortLe().toInt()
                    val sy = buffer.readShortLe().toInt()
                    val sw = buffer.readShortLe().toInt()
                    val sh = buffer.readShortLe().toInt()
                    currentSprite.add(Tile(currentSheet, sx, sy, sw, sh))
                    coordinates.add(Pair(dx,dy))
                }
                else -> {
                    error("Unhandled mode byte. $sheetMode")
                }
            }
            sheetMode = buffer.readByte().toInt()
        }

        return TiledImage(width, height, currentSprite, coordinates)
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

    fun getSprite(name: String?): TiledImage? {
        if(name == null) {
            return null
        }
        if(!names.contains(name)) {
            return null
        }
        return buildSprite(names.indexOf(name))
    }
}