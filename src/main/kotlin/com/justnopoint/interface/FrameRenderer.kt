package com.justnopoint.`interface`

import javafx.scene.canvas.GraphicsContext

interface FrameRenderer {
    fun renderFrame(g: GraphicsContext, frame: Frame, axisX: Int, axisY: Int, zoom: Double)
    fun renderFrameData(g: GraphicsContext, frame: Frame, axisX: Int, axisY: Int, zoom: Double)
}