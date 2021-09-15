package com.justnopoint.`interface`

import javafx.beans.property.Property
import javafx.scene.canvas.GraphicsContext

interface FrameRenderer {
    fun renderFrame(g: GraphicsContext, frame: Frame, axisX: Int, axisY: Int, zoom: Double)
    fun renderFrameData(g: GraphicsContext, frame: Frame, axisX: Int, axisY: Int, zoom: Double)
    fun getObservableProperty(name: String): Property<Boolean>?
}