package com.justnopoint.matsuri

import com.justnopoint.util.readIntLe
import com.justnopoint.util.getIntAt
import java.io.RandomAccessFile

// Anim chunks contain data for single frames which are one or more sprites
// put together with additional metadata such as scale, axis, and rotation
class AnimFile(val raf: RandomAccessFile, node: Node) {
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

    fun getXAxis(frame: RefSpr): Float {
        if(!frame.props.containsKey(AXIS)) {
            return 0f
        }
        val data = frame.props[AXIS]!!

        return Float.fromBits(data.getIntAt(0))
    }

    fun getXScale(frame: RefSpr): Float {
        if(!frame.props.containsKey(SCALE)) {
            return 1.0f
        }
        val data = frame.props[SCALE]!!
        return Float.fromBits(data.getIntAt(0))
    }

    fun getYScale(frame: RefSpr): Float {
        if(!frame.props.containsKey(SCALE)) {
            return 1.0f
        }
        val data = frame.props[SCALE]!!
        return Float.fromBits(data.getIntAt(4))
    }

    fun getYAxis(frame: RefSpr): Float {
        if(!frame.props.containsKey(AXIS)) {
            return 0f
        }
        val data = frame.props[AXIS]!!

        return Float.fromBits(data.getIntAt(4))
    }

    fun getRotation(frame: RefSpr): Float {
        if(!frame.props.containsKey(ROT)) {
            return 0f
        }
        val data = frame.props[ROT]!!

        return Float.fromBits(data.getIntAt(0))
    }

    fun getOpacity(frame: RefSpr): Double {
        if(!frame.props.containsKey(RGBA_ADJUST)) {
            return 1.0
        }
        val data = frame.props[RGBA_ADJUST]!!

        return (data[3].toInt() and 0xFF) / 255.0
    }

    fun getAnimFrame(name: String?): AnimFrame? {
        if(name == null) {
            return null
        }
        if(names.indexOf(name) == -1) {
            return null
        }

        val index = names.indexOf(name)
        raf.seek(offsets[index])
        val mode = raf.readUnsignedByte()
        if(mode < 2) {
            println("Spr count less than 0! $mode at ${raf.filePointer}")
        }
        val head = ByteArray(20 + 4*mode)
        raf.read(head)
        val refs = (0 until (mode-2)).map { raf.readRef() }
        if(index < (offsets.size-1)) {
            if((offsets[index+1] - raf.filePointer) > 13) {
                println("Possible read underrun, file pointer mismatch at ${raf.filePointer}")
            }
        }
        return AnimFrame(head, refs)
    }

    companion object {
        // Labels for known properties
        const val AXIS = 0x14
        const val ROT = 0x15
        const val SCALE = 0x16
        const val RGBA_ADJUST = 0x1E
    }
}

fun RandomAccessFile.readRef(): RefSpr {
    val head = ByteArray(9)
    read(head)
    val propCount = readUnsignedByte()
    readByte() //One
    val ref = ByteArray(readUnsignedByte())
    read(ref)
    val ref2 = ByteArray(readUnsignedByte())
    read(ref2)
    val props = (0 until propCount).associate {
        val key = readUnsignedByte()
        val value = when(key) {
            AnimFile.AXIS -> ByteArray(8)
            AnimFile.ROT -> ByteArray(4)
            AnimFile.SCALE -> ByteArray(8)
            AnimFile.RGBA_ADJUST -> ByteArray(4)
            0x28 -> ByteArray(1)
            0x46 -> ByteArray(1)
            0x32 -> ByteArray(2)
            0x00 -> ByteArray(1)
            else -> {
                println("No case for prop $key at position $filePointer")
                ByteArray(0)
            }
        }
        read(value)
        key to value
    }

    return RefSpr(head, String(ref), String(ref2), props)
}

data class AnimFrame(val head: ByteArray, val refs: List<RefSpr>)

data class RefSpr(val head: ByteArray, val ref1: String, val ref2: String, val props: Map<Int, ByteArray>)