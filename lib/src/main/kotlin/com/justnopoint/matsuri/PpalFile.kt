package com.justnopoint.matsuri

import okio.FileHandle
import okio.buffer

// Ppal chunk contains PNG palettes.  Used with Pidx chunks.
class PpalFile(raf: FileHandle, node: Node) {
    val buffer = raf.source().buffer()
    val names = ArrayList<String>()
    val palettes = ArrayList<ByteArray>()

    init {
        raf.reposition(buffer, node.offset)

        for(n in 0 until node.extra) {
            val data = ByteArray(buffer.readIntLe())
            val name = buffer.readByteArray(buffer.readByte().toLong())
            buffer.read(data)
            names.add(String(name))
            palettes.add(data)
        }
    }

    fun getPalette(name: String): ByteArray {
        return palettes[names.indexOf(name)]
    }
}