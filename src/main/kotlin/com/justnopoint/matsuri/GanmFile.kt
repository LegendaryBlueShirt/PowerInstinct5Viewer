package com.justnopoint.matsuri

import com.justnopoint.util.getShortAt
import okio.FileHandle
import okio.buffer
import kotlin.collections.HashMap

// Ganm chunks contain definitions for character animation and behavior
// Data is inlined by a series of key/value pairs with an index table at the top
class GanmFile(val raf: FileHandle, node: Node) {
    val buffer = raf.source().buffer()
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
        const val VEL_X = 9
        const val VEL_X_AND_Y = 12
        const val EFFECT_BIND = 17
        const val GRAVITY = 21 //This one's a bit special, for air specials, this property will turn gravity back on
            // It seems that gravity applying normally to air normals may be hardcoded behavior
        const val POSADD_X = 22
        const val INVULNERABILITY = 23
        const val CANCEL = 24
        const val POSADD_Y = 40
        const val EFFECT = 50
        const val EFFECT_ROTATE = 51
        const val EFFECT_FADE_IN = 52
        const val EFFECT_SCALE = 53
        const val BIND = 56
        const val ARMOR = 58
        const val AFTERIMAGES = 59

        fun knownValues(): List<Int> {
            return listOf(LOOP_START, HITDEF, SND, BIND, EFFECT_BIND, EFFECT, EFFECT_ROTATE, GRAVITY,
                EFFECT_FADE_IN, EFFECT_SCALE, CANCEL, ARMOR, AFTERIMAGES, VEL_X, VEL_X_AND_Y,
                POSADD_X, POSADD_Y, INVULNERABILITY)
        }
    }

    init {
        raf.reposition(buffer, node.offset)
        val chunkSize = buffer.readIntLe()
        val str = buffer.readByteArray(buffer.readByte().toLong())
        prefix = String(str)
        val pointer = raf.position(buffer)

        offsets[0] = pointer+buffer.readIntLe()
        var index = 1
        while(raf.position(buffer) < offsets[0]!!) {
            offsets[index++] = pointer+buffer.readIntLe()
        }

        offsets[index] = -1
    }

    fun getAnim(index: Int): List<GanmFrame> {
        if(offsets[index] == -1L) {
            return getSpecialAnim()
        }
        raf.reposition(buffer, offsets[index]!!)
        var props = HashMap<Int, ByteArray>()
        val anim = mutableListOf<GanmFrame>()
        loop@ while(true) {
            val mode = buffer.readShortLe().toInt()
            when(mode and 0xFF) {
                LOOP_END, ANIM_END -> break@loop
                NO_FRAME -> {
                    anim.add(GanmFrame(-1, buffer.readShortLe().toInt(), props))
                    props = HashMap()
                }
                FRAME -> {
                    anim.add(GanmFrame(buffer.readIntLe(), buffer.readShortLe().toInt(), props))
                    props = HashMap()
                }
                VEL_X, 13, 14, EFFECT_SCALE, 54 -> {
                    val data = buffer.readByteArray(4)
                    props[mode] = data
                }
                VEL_X_AND_Y, 16 -> {
                    val data = buffer.readByteArray(12)
                    props[mode] = data
                }
                EFFECT_BIND -> { // Generate effect?
                    val data = buffer.readByteArray(10) //Wrong for Annie 74
                    props[mode] = data
                }
                29 -> { //Unknown behavior
                    //29 Found in Annie anim 79
                    val data = buffer.readByteArray(18)
                    props[mode] = data
                }
                27, EFFECT -> {
                    //27 Found in Annie anim 116
                    val data = buffer.readByteArray(16)
                    props[mode] = data
                }
                HITDEF, SND, POSADD_X, POSADD_Y, EFFECT_ROTATE, EFFECT_FADE_IN -> {
                    val data = buffer.readByteArray(2)
                    props[mode] = data
                }
                BIND -> { //Throw anchor
                    val frame = buffer.readShortLe().toInt()
                    val data = buffer.readByteArray(16)
                    props[mode] = data
                    anim.add(GanmFrame(frame, buffer.readShortLe().toInt(), props))
                    props = HashMap()
                }
                LOOP_START, GRAVITY, ARMOR, CANCEL, AFTERIMAGES -> {
                    props[mode] = ByteArray(0)
                }
                else -> {
                    println("Unhandled property $mode")
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

data class Effect(val index: Int, val axisx: Int, val axisy: Int, val sx: Int, val sy: Int, val sw: Int, val sh: Int, val unk1: Int, val source: Int)
data class Helper(val ref: Int, val x: Int, val y: Int)
data class Bind(val frame: Int, val anchorPoint: Int, val axisx: Int, val axisy: Int)

fun GanmFrame.getHelperSpawn(): Helper? {
    if(!props.containsKey(GanmFile.EFFECT_BIND)) {
        return null
    }
    val data = props[GanmFile.EFFECT_BIND]!!
    return Helper(data.getShortAt(2), data.getShortAt(4).toShort().toInt(), data.getShortAt(6).toShort().toInt())
}

fun GanmFrame.getEffects(): List<Effect> {
    return props.entries.filter { it.key and 0xFF == GanmFile.EFFECT }.map { entry ->
        val data = entry.value
        Effect(entry.key ushr 8,
            data.getShortAt(0).toShort().toInt(), data.getShortAt(2).toShort().toInt(),
            data.getShortAt(4), data.getShortAt(6),
            data.getShortAt(8), data.getShortAt(10),
            data.getShortAt(12), data.getShortAt(14))
    }
}

fun GanmFrame.getEffectScale(effect: Effect): Pair<Double, Double> {
    val key = (effect.index shl 8) or GanmFile.EFFECT_SCALE
    if(!props.containsKey(key)) {
        return Pair(1.0, 1.0)
    }
    val data = props[key]!!
    return Pair(data.getShortAt(0)/256.0, data.getShortAt(2)/256.0)
}

fun GanmFrame.getEffectRotation(effect: Effect): Double {
    val key = (effect.index shl 8) or GanmFile.EFFECT_ROTATE
    if(!props.containsKey(key)) {
        return 0.0
    }
    val data = props[key]!!
    return data.getShortAt(0) * 1.0
}

fun GanmFrame.getBoundEnemy(): Bind? {
    if(!props.containsKey(GanmFile.BIND)) {
        return null
    }
    val data = props[GanmFile.BIND]!!
    return Bind(data.getShortAt(0), data.getShortAt(2), data.getShortAt(4).toShort().toInt(), data.getShortAt(6).toShort().toInt())
}

fun GanmFrame.getSound(): ByteArray? {
    if(!props.containsKey(GanmFile.SND)) {
        return null
    }
    return props[GanmFile.SND]!!
}

fun GanmFrame.getArmor(): Boolean? {
    return props.entries.find { it.key and 0xFF == GanmFile.ARMOR }?.let { it.key ushr 8 and 0xFF != 0 }
}

fun GanmFrame.getCancel(): Boolean? {
    return props.entries.find { it.key and 0xFF == GanmFile.CANCEL }?.let { it.key ushr 8 and 0xFF != 0 }
}

fun GanmFrame.getAfterimage(): Boolean? {
    return props.entries.find { it.key and 0xFF == GanmFile.AFTERIMAGES }?.let { it.key ushr 8 and 0xFF != 0 }
}

fun GanmFrame.getInvulnerability(): Boolean? {
    return props.entries.find { it.key and 0xFF == GanmFile.INVULNERABILITY }?.let { it.key ushr 8 and 0xFF == 0 }
}

fun GanmFrame.getGravity(): Int? {
    return props.entries.find { it.key and 0xFF == GanmFile.GRAVITY }?.key
}

fun GanmFrame.getPosaddX(): Int? {
    if(!props.containsKey(GanmFile.POSADD_X)) {
        return null
    }
    val data = props[GanmFile.POSADD_X]!!
    return data.getShortAt(0)
}

fun GanmFrame.getPosaddY(): Int? {
    if(!props.containsKey(GanmFile.POSADD_Y)) {
        return null
    }
    val data = props[GanmFile.POSADD_Y]!!
    return data.getShortAt(0)
}

fun GanmFrame.getVelX(): Int? {
    return if(props.containsKey(GanmFile.VEL_X)) {
        val data = props[GanmFile.VEL_X]!!
        data.getShortAt(2) //Unknown what the first two bytes are for
    } else if(props.containsKey(GanmFile.VEL_X_AND_Y)) {
        val data = props[GanmFile.VEL_X_AND_Y]!!
        data.getShortAt(2) //Unknown what the first two bytes are for
    } else {
        null
    }
}

fun GanmFrame.getVelY(): Int? {
    return if(props.containsKey(GanmFile.VEL_X_AND_Y)) {
        val data = props[GanmFile.VEL_X_AND_Y]!!
        data.getShortAt(6) //Unknown what the two preceding bytes are for
    } else {
        null
    }
}

fun Effect.toSprite(sheets: ImagFile, hanyou: ImagFile): GrapFile.Sprite {
    val sourceIndex = source-1
    println(sourceIndex)
    // This works for Annie but probably needs tweaking, or we're missing entire sheets
    val sheet = sheets.getSheet(sourceIndex)?:hanyou.getSheet(sourceIndex-4)?:return GrapFile.Sprite(0, 0, IntArray(0))
    val raster = IntArray(sw*sh*4)
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
        System.arraycopy(line, 0, raster, y*sw*4, line.size)
    }
    return GrapFile.Sprite(sw, sh, raster)
}