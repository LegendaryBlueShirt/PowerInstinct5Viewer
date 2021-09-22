package com.justnopoint.interfaces

interface FrameRenderer {
    fun getProperties(): List<String>
    fun getRenderableSprites(frame: Frame, props: HashMap<String, Boolean>): List<RenderableSprite>
    fun getRenderableText(frame: Frame, props: HashMap<String, Boolean>): List<RenderableText>
    fun getRenderableBoxes(frame: Frame, props: HashMap<String, Boolean>): List<RenderableBox>
}

data class RenderableSprite(val image: TiledImage, val axisX: Int, val axisY: Int,
                            val scaleX: Double = 1.0, val scaleY: Double = 1.0, val rotation: Double = 0.0,
                            val opacity: Double = 1.0)
data class RenderableText(val text: String, val positionX: Int, val positionY: Int)
data class RenderableBox(val x: Int, val y: Int, val width: Int, val height: Int,
                         val color: Triple<Double, Double, Double>)