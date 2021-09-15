package com.justnopoint.matsuri

import com.justnopoint.util.readIntLe
import com.justnopoint.util.readShortLe
import com.justnopoint.util.getShortAt
import java.awt.image.*
import java.io.RandomAccessFile
import kotlin.collections.HashMap

class GanmFile(val raf: RandomAccessFile, node: Node) {
    val offsets = HashMap<Int, Long>()
    val prefix: String

    companion object {
        const val NO_FRAME = 0
        const val FRAME = 1
        const val LOOP_START = 2
        const val LOOP_END = 3
        const val ANIM_END = 4
        const val HITDEF = 7
        const val SND = 8
        const val EFFECT = 17
        const val BIND = 56

        fun knownValues(): List<Int> {
            return listOf(LOOP_START, HITDEF, SND, BIND)
        }
    }

    init {
        raf.seek(node.offset)
        val chunkSize = raf.readIntLe()
        val str = ByteArray(raf.readUnsignedByte())
        raf.read(str)
        prefix = String(str)
        val pointer = raf.filePointer

        offsets[0] = pointer+raf.readIntLe()
        var index = 1
        while(raf.filePointer < offsets[0]!!) {
            offsets[index++] = pointer+raf.readIntLe()
        }

        offsets[index] = -1
    }

    fun getAnim(index: Int): List<GanmFrame> {
        if(offsets[index] == -1L) {
            return getSpecialAnim()
        }
        raf.seek(offsets[index]!!)
        var props = HashMap<Int, ByteArray>()
        val anim = mutableListOf<GanmFrame>()
        loop@ while(true) {
            val mode = raf.readShortLe()
            when(mode and 0xFF) {
                LOOP_END, ANIM_END -> break@loop
                NO_FRAME -> {
                    anim.add(GanmFrame(-1, raf.readShortLe(), props))
                    props = HashMap()
                }
                FRAME -> {
                    anim.add(GanmFrame(raf.readIntLe(), raf.readShortLe(), props))
                    props = HashMap()
                }
                9, 13 -> {
                    val data = ByteArray(4)
                    raf.read(data)
                    props[mode] = data
                }
                12, 16 -> {
                    val data = ByteArray(12)
                    raf.read(data)
                    props[mode] = data
                }
                EFFECT -> { // Generate effect?
                    val data = ByteArray(10) //Wrong for Annie 74
                    raf.read(data)
                    props[mode] = data
                }
                29 -> { //Unknown behavior
                    //Found in Annie anim 79
                    val data = ByteArray(18)
                    raf.read(data)
                    props[mode] = data
                }
                50 -> {
                    val data = ByteArray(16)
                    raf.read(data)
                    props[mode] = data
                }
                HITDEF, SND, 22, 40, 51, 52 -> {
                    val data = ByteArray(2)
                    raf.read(data)
                    props[mode] = data
                }
                53, 54 -> {
                    val data = ByteArray(4)
                    raf.read(data)
                    props[mode] = data
                }
                BIND -> { //Throw anchor
                    //Found in Annie 67 (grab)
                    val frame = raf.readShortLe()
                    val data = ByteArray(16)
                    raf.read(data)
                    props[mode] = data
                    anim.add(GanmFrame(frame, raf.readShortLe(), props))
                    props = HashMap()
                }
                LOOP_START, 24, 38, 44 -> {
                    props[mode] = ByteArray(0)
                }
                else -> {
                    props[mode] = ByteArray(0)
                }
            }
        }

        return anim
    }

    fun getSpecialAnim(): List<GanmFrame> {
        return (0 until 25).map {
            GanmFrame(9000+it, 1, HashMap())
        }
    }
}

data class GanmFrame(val frame: Int, val duration: Int, val props: HashMap<Int, ByteArray>)

data class Effect(val axisx: Int, val axisy: Int, val sx: Int, val sy: Int, val sw: Int, val sh: Int, val unk1: Int, val source: Int)
data class Helper(val ref: Int, val x: Int, val y: Int)
data class Bind(val frame: Int, val anchorPoint: Int, val axisx: Int, val axisy: Int)

fun GanmFrame.getHelperSpawn(): Helper? {
    if(!props.containsKey(GanmFile.EFFECT)) {
        return null
    }
    val data = props[GanmFile.EFFECT]!!
    return Helper(data.getShortAt(2), data.getShortAt(4), data.getShortAt(6))
}

fun GanmFrame.getEffects(): List<Effect> {
    return props.entries.filter { it.key and 0xFF == 0x32 }.map { entry ->
        val data = entry.value
        Effect(data.getShortAt(0).toShort().toInt(), data.getShortAt(2).toShort().toInt(),
            data.getShortAt(4), data.getShortAt(6),
            data.getShortAt(8), data.getShortAt(10),
            data.getShortAt(12), data.getShortAt(14))
    }
}

fun GanmFrame.getBoundEnemy(): Bind? {
    if(!props.containsKey(GanmFile.BIND)) {
        return null
    }
    val data = props[GanmFile.BIND]!!
    return Bind(data.getShortAt(0), data.getShortAt(2), data.getShortAt(4).toShort().toInt(), data.getShortAt(6).toShort().toInt())
}

fun Effect.toBufferedImage(sheets: ImagFile): BufferedImage {
    val sourceIndex = source-1
    val sheet = sheets.getSheet(sourceIndex)?:return BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB)
    return BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB).apply {
        val raster = raster
        val plte = if(sheet.getImageLine(0).imgInfo.indexed) {
            sheets.palettes[sheets.names.find { it.endsWith("$sourceIndex") }]!!
        } else {
            null
        }
        val trns = if(sheet.getImageLine(0).imgInfo.indexed) {
            sheets.trans[sheets.names.find { it.endsWith("$sourceIndex") }]!!
        } else {
            null
        }
        sheet.getRect(sx, sy, sw, sh, plte, trns).forEachIndexed { y, line ->
            raster.setPixels(0, y, sw, 1, line)
        }
    }
}