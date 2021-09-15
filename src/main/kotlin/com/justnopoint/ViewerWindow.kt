package com.justnopoint

import com.justnopoint.`interface`.Frame
import com.justnopoint.`interface`.FrameDataProvider
import com.justnopoint.`interface`.FrameRenderer
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.stage.WindowEvent

class ViewerWindow: Canvas() {
    var currentX = 400
    var currentY = 450
    var currentZoom = 3.0

    override fun isResizable(): Boolean {
        return true
    }

    override fun prefWidth(height: Double): Double {
        return width
    }

    override fun prefHeight(width: Double): Double {
        return height
    }

    @Volatile
    var running = false
    var background = Color.BLACK

    var rendererProvider: FrameDataProvider? = null
    var currentFrame: Frame? = null

    fun prepareForRendering() {
        running = true
    }

    fun render() {
        val g = graphicsContext2D
        g.fill = background
        g.fillRect(0.0, 0.0, width, height)

        currentFrame?.let {
            rendererProvider?.getFrameRenderer()?.renderFrame(g = g, frame = it, axisY = getAxisY(), axisX = getAxisX(), zoom = currentZoom)
            rendererProvider?.getFrameRenderer()?.renderFrameData(g = g, frame = it, axisY = getAxisY(), axisX = getAxisX(), zoom = currentZoom)
        }
    }

    fun getWindowCloseHandler(): EventHandler<WindowEvent> {
        return EventHandler {
            running = false
            try {
                Thread.sleep(100)
            } catch (e1: InterruptedException) {
                // TODO Auto-generated catch block
                e1.printStackTrace()
            }
        }
    }

    private fun getAxisX(): Int {
        return if (dragging) {
            (displaceX - clickOriginX).toInt() + currentX
        } else {
            currentX
        }
    }

    private fun getAxisY(): Int {
        return if (dragging) {
            (displaceY - clickOriginY).toInt() + currentY
        } else {
            currentY
        }
    }

    fun resetPosition() {
        currentX = (width / 2).toInt()
        currentY = (this.height / 2 + 150).toInt()
    }

    var dragging = false
    var clickOriginX = 0.0
    var clickOriginY = 0.0
    var displaceX = 0.0
    var displaceY = 0.0
    fun onClick(event: MouseEvent) {
        if (event.button === MouseButton.PRIMARY) {
            dragging = true
            clickOriginX = event.screenX
            clickOriginY = event.screenY
            displaceX = clickOriginX
            displaceY = clickOriginY
        }
    }

    fun onDrag(event: MouseEvent) {
        if (dragging) {
            displaceX = event.screenX
            displaceY = event.screenY
        }
    }

    fun onRelease(event: MouseEvent) {
        if (dragging) {
            if (event.button === MouseButton.PRIMARY) {
                currentX = getAxisX()
                currentY = getAxisY()
                dragging = false
            }
        }
    }
}