package com.justnopoint.matsuri

import ar.com.hjg.pngj.*
import ar.com.hjg.pngj.chunks.PngChunkPLTE
import ar.com.hjg.pngj.chunks.PngChunkTRNS
import com.justnopoint.util.readIntLe
import java.io.*

class ImagFile {
    val names = ArrayList<String>()
    val tileSheets = ArrayList<ImageLineSetDefault<ImageLineInt>>()
    val palettes = HashMap<String, PngChunkPLTE>()
    val trans = HashMap<String, PngChunkTRNS>()

    fun load(raf: RandomAccessFile, node: Node, ppal: PpalFile? = null) {
        raf.seek(node.offset)
        if(ppal == null) {
            loadImag(raf, node.extra)
        } else {
            loadPidx(raf, node.extra, ppal)
        }
    }

    fun loadPidx(raf: RandomAccessFile, nSheets: Int, ppal: PpalFile) {
        for(n in 0 until nSheets) {
            val chunkSize = raf.readIntLe()
            var name = ByteArray(raf.readUnsignedByte())
            raf.read(name)
            val sheetName = String(name)
            names.add(sheetName)
            name = ByteArray(raf.readUnsignedByte())
            raf.read(name)
            val plte = ppal.getPalette(String(name))
            val header = readBlock(raf)
            val pngData = readBlock(raf)
            val reader = generatePng(listOf(header, plte, pngData))
            tileSheets.add(reader.readRows() as ImageLineSetDefault<ImageLineInt>)
            if(reader.imgInfo.indexed) {
                palettes[sheetName] = reader.chunksList.getById1(PngChunkPLTE.ID) as PngChunkPLTE
                trans[sheetName] = reader.chunksList.getById1(PngChunkTRNS.ID) as PngChunkTRNS
            }
        }
    }

    fun loadImag(raf: RandomAccessFile, nSheets: Int) {
        for(n in 0 until nSheets) {
            val chunkSize = raf.readIntLe()
            val name = ByteArray(raf.readUnsignedByte())
            raf.read(name)
            names.add(n, String(name))
            val header = readBlock(raf)
            val pngData = readBlock(raf)
            tileSheets.add(n, generatePng(listOf(header, pngData)).readRows() as ImageLineSetDefault<ImageLineInt>)
        }
    }

    private fun readBlock(raf: RandomAccessFile): ByteArray {
        val data = ByteArray(raf.readIntLe())
        raf.read(data)
        return data
    }

    private fun generatePng(chunks: List<ByteArray>): PngReader {
        val baos = ByteArrayOutputStream()
        baos.write(PNG)
        chunks.forEach {
            baos.write(it)
        }
        baos.write(IEND)
        return PngReader(ByteArrayInputStream(baos.toByteArray()))
    }

    fun getSheet(index: Int): ImageLineSetDefault<ImageLineInt>? {
        names.find { it.endsWith("$index") }?.let {
            return getSheet(it)
        }?: return null
    }

    fun getSheet(name: String): ImageLineSetDefault<ImageLineInt> {
        //println(name)
        return tileSheets[names.indexOf(name)]
    }

    companion object {
        val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        val IEND = byteArrayOf(
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
        )
    }
}

fun ImageLineSetDefault<ImageLineInt>.getRect(sx: Int, sy: Int, sw: Int, sh: Int, plte: PngChunkPLTE? = null, trns: PngChunkTRNS? = null): List<IntArray> {
    //println("$sx $sy $sw $sh")
    return (sy until sy+sh).map { y ->
        val scanline = if(plte != null) {
            ImageLineHelper.palette2rgb(getImageLine(y), plte, trns, null)
        } else {
            getImageLine(y).scanline
        }
        scanline.sliceArray(sx * 4 until (sx + sw) * 4)
    }
}