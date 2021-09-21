package com.justnopoint.matsuri

import ar.com.hjg.pngj.*
import ar.com.hjg.pngj.chunks.PngChunkPLTE
import ar.com.hjg.pngj.chunks.PngChunkTRNS
import okio.Buffer
import okio.BufferedSource
import okio.FileHandle
import okio.buffer

// Imag chunks contain image data as PNGs.
// This class also loads Pidx chunks, which contain indexed PNGs.
class ImagFile {
    val names = ArrayList<String>()
    val tileSheets = ArrayList<ImageLineSetDefault<ImageLineInt>>()
    val palettes = HashMap<String, PngChunkPLTE>()
    val trans = HashMap<String, PngChunkTRNS>()

    fun load(handle: FileHandle, node: Node, ppal: PpalFile? = null) {
        val buffer = handle.source().buffer()
        handle.reposition(buffer, node.offset)
        if(ppal == null) {
            loadImag(buffer, node.extra)
        } else {
            loadPidx(buffer, node.extra, ppal)
        }
    }

    fun loadPidx(source: BufferedSource, nSheets: Int, ppal: PpalFile) {
        for(n in 0 until nSheets) {
            val chunkSize = source.readIntLe()
            var name = ByteArray(source.readByte().toInt())
            source.read(name)
            val sheetName = String(name)
            names.add(sheetName)
            name = ByteArray(source.readByte().toInt())
            source.read(name)
            val plte = ppal.getPalette(String(name))
            val header = source.readByteArray(source.readIntLe().toLong())
            val pngData = source.readByteArray(source.readIntLe().toLong())
            val reader = generatePng(listOf(header, plte, pngData))
            tileSheets.add(reader.readRows() as ImageLineSetDefault<ImageLineInt>)
            if(reader.imgInfo.indexed) {
                palettes[sheetName] = reader.chunksList.getById1(PngChunkPLTE.ID) as PngChunkPLTE
                trans[sheetName] = reader.chunksList.getById1(PngChunkTRNS.ID) as PngChunkTRNS
            }
        }
    }

    fun loadImag(source: BufferedSource, nSheets: Int) {
        for(n in 0 until nSheets) {
            val chunkSize = source.readIntLe()
            val name = source.readByteArray(source.readByte().toLong())
            println(String(name))
            names.add(n, String(name))
            val header = source.readByteArray(source.readIntLe().toLong())
            val pngData = source.readByteArray(source.readIntLe().toLong())
            tileSheets.add(n, generatePng(listOf(header, pngData)).readRows() as ImageLineSetDefault<ImageLineInt>)
        }
    }

    private fun generatePng(chunks: List<ByteArray>): PngReader {
        val buffer = Buffer()
        val baos = buffer.outputStream()
        baos.write(PNG)
        chunks.forEach {
            baos.write(it)
        }
        baos.write(IEND)
        return PngReader(buffer.inputStream())
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