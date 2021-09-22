package com.justnopoint.matsuri

import com.justnopoint.interfaces.ImageBank
import okio.Buffer
import okio.BufferedSource
import okio.FileHandle
import okio.buffer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// Imag chunks contain image data as PNGs.
// This class also loads Pidx chunks, which contain indexed PNGs.
class ImagFile: KoinComponent {
    val bank: ImageBank by inject()
    val names = ArrayList<String>()
    val handles = ArrayList<Any>()

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
            var name = source.readByteArray(source.readByte().toLong())
            val sheetName = String(name)
            names.add(sheetName)
            name = source.readByteArray(source.readByte().toLong())
            val plte = ppal.getPalette(String(name))
            val header = source.readByteArray(source.readIntLe().toLong())
            val pngData = source.readByteArray(source.readIntLe().toLong())
            handles.add(generatePng(listOf(header, plte, pngData)))
        }
    }

    fun loadImag(source: BufferedSource, nSheets: Int) {
        for(n in 0 until nSheets) {
            val chunkSize = source.readIntLe()
            val name = source.readByteArray(source.readByte().toLong())
            names.add(String(name))
            val header = source.readByteArray(source.readIntLe().toLong())
            val pngData = source.readByteArray(source.readIntLe().toLong())
            handles.add(generatePng(listOf(header, pngData)))
        }
    }

    private fun generatePng(chunks: List<ByteArray>): Any {
        val buffer = Buffer()
        val baos = buffer.outputStream()
        baos.write(PNG)
        chunks.forEach {
            baos.write(it)
        }
        baos.write(IEND)
        return bank.loadAndStoreImage(buffer.readByteArray(buffer.size))
    }

    fun getSheet(index: Int): Any? {
        names.find { it.endsWith("$index") }?.let {
            return getSheet(it)
        }?: return null
    }

    fun getSheet(name: String): Any {
        return handles[names.indexOf(name)]
    }

    companion object {
        val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        val IEND = byteArrayOf(
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
        )
    }
}