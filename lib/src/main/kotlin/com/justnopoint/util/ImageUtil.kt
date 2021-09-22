package com.justnopoint.util

import java.awt.Point
import java.awt.image.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun createIndexColorModel(data: ByteArray?): IndexColorModel {
    val buffer = data?.copyOf()?: ByteArray(1024)
    for(m in 0 until 256) {
        var temp1 = buffer[m*4]
        buffer[m*4] = buffer[m*4+3]
        buffer[m*4+3] = temp1
        temp1 = buffer[m*4+1]
        buffer[m*4+1] = buffer[m*4+2]
        buffer[m*4+2] = temp1
    }

    return IndexColorModel(8, 256, buffer, 0, true, 0)
}

fun createIndexColorModelBGRA(data: ByteArray?): IndexColorModel {
    val buffer = data?.copyOf()?: ByteArray(1024)
    for(m in 0 until 256) {
        val temp1 = buffer[m*4]
        buffer[m*4] = buffer[m*4+2]
        buffer[m*4+2] = temp1
    }

    return IndexColorModel(8, 256, buffer, 0, true, 0)
}

fun createBufferedImage(data: ByteArray, width: Int, height: Int): BufferedImage {
    val intBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
    val intArray = IntArray(intBuffer.remaining())
    intBuffer.get(intArray)
    val buffer = DataBufferInt(intArray, width*height)
    val colorModel = ColorModel.getRGBdefault()
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val writableRaster = WritableRaster.createWritableRaster(sampleModel, buffer, Point(0,0))
    return BufferedImage(colorModel, writableRaster, false, null)
}

fun createBufferedImage(data: IntArray, width: Int, height: Int): BufferedImage {
    val buffer = DataBufferInt(data, width*height)
    val colorModel = ColorModel.getRGBdefault()
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val writableRaster = WritableRaster.createWritableRaster(sampleModel, buffer, Point(0,0))
    return BufferedImage(colorModel, writableRaster, false, null)
}

fun createBufferedImage(data: ByteArray, width: Int, height: Int, colorModel: IndexColorModel): BufferedImage {
    val buffer = DataBufferByte(data, width*height)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val writableRaster = WritableRaster.createWritableRaster(sampleModel, buffer, Point(0,0))
    return BufferedImage(colorModel, writableRaster, false, null)
}