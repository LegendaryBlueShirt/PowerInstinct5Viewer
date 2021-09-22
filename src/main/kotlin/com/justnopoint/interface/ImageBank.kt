package com.justnopoint.`interface`

interface ImageBank {
    fun loadAndStoreImage(data: ByteArray): Any
}

data class TiledImage(val width: Int, val height: Int, val tiles: List<Tile>, val coordinates: List<Pair<Int, Int>>)
data class Tile(val key: Any, val sx: Int, val sy: Int, val sw: Int, val sh: Int)