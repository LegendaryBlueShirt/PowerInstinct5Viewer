package com.justnopoint.matsuri

import com.justnopoint.util.readIntLe
import java.io.RandomAccessFile

class PpalFile(raf: RandomAccessFile, node: Node) {
    val names = ArrayList<String>()
    val palettes = ArrayList<ByteArray>()

    init {
        raf.seek(node.offset)

        for(n in 0 until node.extra) {
            val data = ByteArray(raf.readIntLe())
            val name = ByteArray(raf.readUnsignedByte())
            raf.read(name)
            raf.read(data)
            names.add(String(name))
            palettes.add(data)
        }
    }

    fun getPalette(name: String): ByteArray {
        return palettes[names.indexOf(name)]
    }
}