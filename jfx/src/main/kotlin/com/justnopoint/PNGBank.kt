package com.justnopoint

import ar.com.hjg.pngj.ImageLineHelper
import ar.com.hjg.pngj.ImageLineInt
import ar.com.hjg.pngj.ImageLineSetDefault
import ar.com.hjg.pngj.PngReader
import ar.com.hjg.pngj.chunks.PngChunkPLTE
import ar.com.hjg.pngj.chunks.PngChunkTRNS
import com.justnopoint.interfaces.ImageBank
import com.justnopoint.interfaces.Tile
import com.justnopoint.interfaces.TiledImage
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream

class PNGBank: ImageBank {
    private val textures = mutableListOf<ImageLineSetDefault<ImageLineInt>>()
    private val palettes = HashMap<Int, PngChunkPLTE>()
    private val trans = HashMap<Int, PngChunkTRNS>()

    override fun loadAndStoreImage(data: ByteArray): Int {
        val reader = PngReader(ByteArrayInputStream(data))
        textures.add(reader.readRows() as ImageLineSetDefault<ImageLineInt>)
        if(reader.imgInfo.indexed) {
            palettes[textures.size] = reader.chunksList.getById1(PngChunkPLTE.ID) as PngChunkPLTE
            trans[textures.size] = reader.chunksList.getById1(PngChunkTRNS.ID) as PngChunkTRNS
        }
        return textures.size
    }

    fun getTile(tile: Tile): List<IntArray> {
        val tex = textures[(tile.key as Int) -1]
        val plte = palettes[tile.key]
        val trns = trans[tile.key]
        println("${tile.key} ${tile.sx} ${tile.sy} ${tile.sw} ${tile.sh}")
        return (tile.sy until tile.sy+tile.sh).map { y ->
            val scanline = if(plte != null) {
                ImageLineHelper.palette2rgb(tex.getImageLine(y), plte, trns, null)
            } else {
                tex.getImageLine(y).scanline
            }
            scanline.sliceArray(tile.sx * 4 until (tile.sx + tile.sw) * 4)
        }
    }

    fun tiledImageToFullImage(tiledImage: TiledImage): BufferedImage {
        val buf = BufferedImage(tiledImage.width, tiledImage.height, BufferedImage.TYPE_INT_ARGB)
        val raster = buf.raster

        tiledImage.tiles.zip(tiledImage.coordinates).forEach {
            getTile(it.first).forEachIndexed { y, line ->
                raster.setPixels(it.second.first, it.second.second+y, it.first.sw, 1, line)
            }
        }

        return buf
    }
}