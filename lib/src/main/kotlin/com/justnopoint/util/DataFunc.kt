package com.justnopoint.util

import java.io.RandomAccessFile

fun ByteArray.getUIntAt(idx: Int) =
    ((this[idx + 3].toUInt() and 0xFFu) shl 24) or
            ((this[idx + 2].toUInt() and 0xFFu) shl 16) or
            ((this[idx + 1].toUInt() and 0xFFu) shl 8) or
            (this[idx].toUInt() and 0xFFu)

fun ByteArray.getIntAt(idx: Int) =
    ((this[idx + 3].toInt() and 0xFF) shl 24) or
            ((this[idx + 2].toInt() and 0xFF) shl 16) or
            ((this[idx + 1].toInt() and 0xFF) shl 8) or
            (this[idx].toInt() and 0xFF)

fun ByteArray.getShortAt(idx: Int) =
    ((this[idx + 1].toInt() and 0xFF) shl 8) or
            (this[idx].toInt() and 0xFF)

fun RandomAccessFile.readLongLe(): Long {
    return readUnsignedByte().toLong() or (readUnsignedByte().toLong() shl 8) or (readUnsignedByte().toLong() shl 16) or (readUnsignedByte().toLong() shl 24)
}

fun RandomAccessFile.readIntLe(): Int {
    return readUnsignedByte() or (readUnsignedByte() shl 8) or (readUnsignedByte() shl 16) or (readUnsignedByte() shl 24)
}

fun RandomAccessFile.readShortLe(): Int {
    return readUnsignedByte() or ((read() shl 24) shr 16)
}

fun RandomAccessFile.readCharLe(): Int {
    return readUnsignedByte() or (readUnsignedByte() shl 8)
}