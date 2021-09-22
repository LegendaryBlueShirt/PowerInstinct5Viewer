package com.justnopoint

import com.justnopoint.`interface`.Frame
import com.justnopoint.`interface`.FrameDataProvider
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.stage.WindowEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ViewerWindow: Canvas(), KoinComponent {
    val bank by inject<PNGBank>()
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

    fun render(props: HashMap<String, Boolean>) {
        val g = graphicsContext2D
        g.fill = background
        g.fillRect(0.0, 0.0, width, height)

        currentFrame?.let {
            g.save()
            g.translate(getAxisX().toDouble(), getAxisY().toDouble())
            g.scale(currentZoom, currentZoom)
            rendererProvider?.getFrameRenderer()?.getRenderableSprites(frame = it, props = props)?.forEach { renderable ->
                val image = bank.tiledImageToFullImage(renderable.image)
                g.save()
                g.translate(renderable.axisX.toDouble(), renderable.axisY.toDouble())
                g.rotate(renderable.rotation)
                g.drawImage(
                    SwingFXUtils.toFXImage(image, null),
                    0.0,
                    0.0,
                    image.width.toDouble(),
                    image.height.toDouble(),
                    0.0,
                    0.0,
                    image.width * renderable.scaleX,
                    image.height * renderable.scaleY
                )
                g.restore()
            }
            rendererProvider?.getFrameRenderer()?.getRenderableBoxes(frame = it, props = props)?.forEach { box ->
                g.stroke = Color(box.color.first, box.color.second, box.color.third, 0.9)
                g.fill = Color(box.color.first, box.color.second, box.color.third, 0.5)
                g.strokeRect(box.x.toDouble(), box.y.toDouble(), box.width.toDouble(), box.height.toDouble())
                g.fillRect(box.x.toDouble(), box.y.toDouble(), box.width.toDouble(), box.height.toDouble())
            }
            g.restore()
            g.save()
            g.font = Font.font(getMonospaceFonts()[0])
            g.fill = Color(1.0, 1.0, 1.0, 1.0)
            rendererProvider?.getFrameRenderer()?.getRenderableText(frame = it, props = props)?.forEach { text ->
                g.fillText(text.text, text.positionX.toDouble(), text.positionY.toDouble())
            }
            g.restore()
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

    private fun getMonospaceFonts(): List<String> {
        val thinTxt = Text("1 l")
        val thickTxt = Text("MWX")

        val fontFamilyList = Font.getFamilies()
        val monospacedFonts = mutableListOf<String>()
        fontFamilyList.forEach {
            val font = Font.font(it, FontWeight.NORMAL, FontPosture.REGULAR, 14.0)
            thinTxt.font = font
            thickTxt.font = font
            if(thinTxt.layoutBounds.width == thickTxt.layoutBounds.width) {
                monospacedFonts.add(it)
            }
        }
        return monospacedFonts
    }
}