package com.justnopoint.matsuri

import com.justnopoint.`interface`.Frame
import com.justnopoint.`interface`.FrameRenderer
import com.justnopoint.util.AnimHelper
import com.justnopoint.util.getShortAt
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import java.io.RandomAccessFile
import java.text.DecimalFormat

class MatsuriFrameRenderer(charFile: RandomAccessFile, effFile: RandomAccessFile?): FrameRenderer {
    var showBoxes = SimpleBooleanProperty(this, "showBoxes", true)
    var showDebug = SimpleBooleanProperty(this, "showDebug", true)
    var hideKnown = SimpleBooleanProperty(this, "hideKnown", false)
    var showAxis = SimpleBooleanProperty(this, "showAxis", true)
    var showBinds = SimpleBooleanProperty(this, "showBinds", true)

    val grapFile: GrapFile
    val frameFile: AnimFile
    val animFile: GanmFile
    val grecFile: GrecFile
    val thwpFile: ThwpFile
    val gatkFile: GatkFile
    val eff = ImagFile()

    init {
        val vsa = VsaFile(charFile)

        grapFile = vsa.getNode(VsaFile.GRAP)?.let { grapNode ->
            vsa.getNode(VsaFile.IMAG)?.let { imagNode ->
                val img = ImagFile().apply {
                    load(charFile, imagNode)
                }
                GrapFile(charFile, grapNode, img)
            }?: error("Could not find IMAG node for vsa file")
        }?: error("Could not find GRAP node for vsa file")
        frameFile = vsa.getNode(VsaFile.ANIM)?.let { animNode ->
            AnimFile(charFile, animNode)
        }?: error("Could not find ANIM node for vsa file")
        animFile = vsa.getNode(VsaFile.GANM)?.let { ganmNode ->
            GanmFile(charFile, ganmNode)
        }?: error("Could not find GANM node for vsa file")
        grecFile = vsa.getNode(VsaFile.GREC)?.let { grecNode ->
            GrecFile(charFile, grecNode)
        }?: error("Could not find GREC node for vsa file")
        thwpFile = vsa.getNode(VsaFile.THWP)?.let { thwpNode ->
            ThwpFile(charFile, thwpNode)
        }?: error("Could not find THWP node for vsa file")
        gatkFile = vsa.getNode(VsaFile.GATK)?.let { gatkNode ->
            GatkFile(charFile, gatkNode)
        }?: error("Could not find GATK node for vsa file")

        effFile?.let {
            val effvsa = VsaFile(effFile)
            effvsa.getNode(VsaFile.IMAG)?.let { imagNode ->
                eff.load(effFile, imagNode)
            }
            effvsa.getNode(VsaFile.PPAL)?.let { ppalNode ->
                effvsa.getNode(VsaFile.PIDX)?.let { pidxNode ->
                    eff.load(effFile, pidxNode, PpalFile(effFile, ppalNode))
                }
            }
            effFile.close()
        }
    }

    override fun getObservableProperty(name: String): Property<Boolean>? {
        return when(name) {
            "showBoxes" -> showBoxes
            "showDebug" -> showDebug
            "showAxis" -> showAxis
            "hideKnown" -> hideKnown
            "showBinds" -> showBinds
            else -> null
        }
    }

    override fun renderFrame(g: GraphicsContext, frame: Frame, axisX: Int, axisY: Int, zoom: Double) {
        if (frame !is MatsuriFrameDataProvider.MatsuriFrame) {
            return
        }
        g.save()

        g.translate(axisX.toDouble(), axisY.toDouble())
        g.scale(zoom, zoom)

        val name = "${animFile.prefix}${frame.ganmFrame.frame}"
        frameFile.getAnimFrame(name)?.let { animFrame ->
            if(showBinds.get()) {
                frame.ganmFrame.getBoundEnemy()?.let {
                    val bindName = "${animFile.prefix}${it.frame}"
                    frameFile.getAnimFrame(bindName)?.let { bindFrame ->
                        grapFile.getSprite(bindFrame.refs[0].ref1)?.let { sprite ->
                            val anchor = thwpFile.getCoords(it.frame - 9000)[it.anchorPoint]
                            val frameAxisX = (frameFile.getXAxis(bindFrame.refs[0]))
                            val frameAxisY = (frameFile.getYAxis(bindFrame.refs[0]))
                            println("${anchor.first},${anchor.second}  ${it.axisx},${it.axisy}  $frameAxisX,$frameAxisY")
                            g.save()
                            g.translate(
                                it.axisx.toDouble() - (frameAxisX - anchor.first),
                                it.axisy.toDouble() + (frameAxisY - anchor.second)
                            )
                            g.drawImage(
                                SwingFXUtils.toFXImage(sprite, null),
                                0.0,
                                0.0,
                                sprite.width.toDouble(),
                                sprite.height.toDouble(),
                                0.0,
                                0.0,
                                sprite.width * (-1).toDouble(),
                                sprite.height.toDouble()
                            )
                            g.restore()
                        } ?: println("Missing sprite reference ${bindFrame.refs[0].ref1}")
                    }
                }
            }
            for (ref in animFrame.refs.reversed()) {
                grapFile.getSprite(ref.ref1)?.let { sprite ->
                    val frameAxisX = (frameFile.getXAxis(ref))
                    val frameAxisY = (frameFile.getYAxis(ref))
                    g.save()
                    g.translate(frameAxisX.toDouble(), frameAxisY.toDouble())
                    g.rotate(frameFile.getRotation(ref).toDouble())
                    g.drawImage(
                        SwingFXUtils.toFXImage(sprite, null),
                        0.0,
                        0.0,
                        sprite.width.toDouble(),
                        sprite.height.toDouble(),
                        0.0,
                        0.0,
                        sprite.width * (frameFile.getXScale(ref)).toDouble(),
                        sprite.height * (frameFile.getYScale(ref)).toDouble()
                    )
                    g.restore()
                } ?: println("Missing sprite reference ${ref.ref1}")
            }
            frame.ganmFrame.getEffects().forEach { effect ->
                val img = effect.toBufferedImage(eff)
                g.save()
                g.translate(effect.axisx.toDouble(), effect.axisy.toDouble())
                g.drawImage(
                    SwingFXUtils.toFXImage(img, null),
                    0.0,
                    0.0,
                    img.width.toDouble(),
                    img.height.toDouble(),
                    0.0,
                    0.0,
                    img.width * (1).toDouble(),
                    img.height * (1).toDouble()
                )
                g.restore()
            }
        }

        g.restore()
    }

    override fun renderFrameData(g: GraphicsContext, frame: Frame, axisX: Int, axisY: Int, zoom: Double) {
        if (frame !is MatsuriFrameDataProvider.MatsuriFrame) {
            return
        }
        g.save()
        if (showAxis.get()) {
            g.save()
            g.translate(axisX.toDouble(), axisY.toDouble())
            g.stroke = Color(0.0, 1.0, 0.0, 1.0)
            g.fill = Color(0.0, 1.0, 0.0, 1.0)
            g.strokeLine(-2.0, 0.0, 2.0, 0.0)
            g.strokeLine(0.0, -2.0, 0.0, 2.0)
            g.restore()

            if (frame.ganmFrame.frame >= 9000) {
                g.stroke = Color(1.0, 0.0, 1.0, 1.0)
                g.fill = Color(1.0, 0.0, 1.0, 1.0)
                thwpFile.getCoords(frame.ganmFrame.frame - 9000).forEachIndexed { i, coords ->
                    g.save()
                    g.translate(axisX.toDouble(), axisY.toDouble())
                    g.scale(zoom, zoom)
                    g.translate(coords.first.toDouble(), coords.second.toDouble())
                    g.strokeLine(-2.0, 0.0, 2.0, 0.0)
                    g.strokeLine(0.0, -2.0, 0.0, 2.0)
                    g.setFont(Font("Helvetica", 5.0));
                    g.strokeText("$i", -6.0,-6.0)
                    g.restore()
                }
            }
        }

        g.fill = Color(1.0, 1.0, 1.0, 1.0)
        val frameTime = AnimHelper.getTimeForFrame(frame.seq, frame.seq.ganmFrames.indexOf(frame.ganmFrame))
        val totalAnimTime = AnimHelper.getSequenceDurationTotal(frame.seq)
        g.fillText("Time $frameTime/$totalAnimTime", 20.0, 20.0)
        g.fillText("Duration ${frame.getDuration()}", 20.0, 40.0)

        frame.ganmFrame.props[GanmFile.HITDEF]?.getShortAt(0)?.let(gatkFile::getHitdef)?.let { hitdef ->
            g.fill = Color(1.0, 0.0, 0.0, 1.0)
            if(showDebug.get()) {
                g.fillText("Hitdef ${bytesToHex(hitdef.data)}", 20.0, 80.0)
            }
            g.fillText("Damage ${hitdef.getDamage()}", 90.0, 20.0)
            val plusMinusNF = DecimalFormat("+#;-#")
            val remaining = totalAnimTime - frameTime
            val hitTime = animFile.getAnim(hitdef.getHitAnim()).sumBy { it.duration }

            //g.fillText("On Block ${plusMinusNF.format(hitdef.getGuardPause() - remaining)}", 90.0, 40.0)
            g.fillText("On Hit ${plusMinusNF.format(hitTime - remaining)}", 90.0, 60.0)
        }

        frame.ganmFrame.getHelperSpawn()?.let {
            g.save()
            g.translate(axisX.toDouble(), axisY.toDouble())
            g.scale(zoom, zoom)
            g.translate(it.x.toDouble(), -it.y.toDouble())
            g.stroke = Color(0.5, 0.5, 0.0, 1.0)
            g.fill = Color(0.5, 0.5, 0.0, 1.0)
            g.strokeLine(-2.0, 0.0, 2.0, 0.0)
            g.strokeLine(0.0, -2.0, 0.0, 2.0)
            g.restore()
        }

        if(showDebug.get()) {
            var drawx = 20.0
            g.fill = Color(1.0, 1.0, 1.0, 1.0)
            frame.ganmFrame.props.forEach prop@{ (key, value) ->
                if (hideKnown.get() && GanmFile.knownValues().contains(key)) {
                    return@prop
                }
                g.fillText("$key ${bytesToHex(value)}", drawx, 320.0)
                drawx += (60 + 14 * value.size)
            }
        }

        val name = "${animFile.prefix}${frame.ganmFrame.frame}"
        frameFile.getAnimFrame(name)?.let { data ->
            if (showDebug.get()) {
                g.fill = Color(1.0, 1.0, 1.0, 1.0)
                g.fillText("$name ${bytesToHex(data.head)}", 20.0, 350.0)
                var posy = 380.0
                for (ref in data.refs) {
                    var posx = 210.0
                    g.fillText("${ref.ref1} ${ref.ref2} ${bytesToHex(ref.head)}", 20.0, posy)
                    ref.props.entries.forEach prop@{ (key, value) ->
                        if (hideKnown.get() && listOf(AnimFile.ROT, AnimFile.SCALE, AnimFile.AXIS).contains(key)) {
                            return@prop
                        }
                        g.fillText("${key}-${bytesToHex(value)}", posx, posy)
                        posx += (60 + 14 * value.size)
                    }
                    posy += 25
                }
            }

            if (showBoxes.get()) {
                g.save()
                g.translate(axisX.toDouble(), axisY.toDouble())
                g.scale(zoom, zoom)
                grecFile.getBoxes(frame.ganmFrame.frame)?.let { boxes ->
                    g.stroke = Color(1.0, 1.0, 1.0, 0.9)
                    g.fill = Color(1.0, 1.0, 1.0, 0.5)
                    boxes.boxtype[0].forEach {
                        g.strokeRect(it.x, it.y, it.width, it.height)
                        g.fillRect(it.x, it.y, it.width, it.height)
                    }
                    g.stroke = Color(1.0, 0.0, 0.0, 0.9)
                    g.fill = Color(1.0, 0.0, 0.0, 0.5)
                    boxes.boxtype[1].forEach {
                        g.strokeRect(it.x, it.y, it.width, it.height)
                        g.fillRect(it.x, it.y, it.width, it.height)
                    }
                    g.stroke = Color(0.0, 0.0, 1.0, 0.9)
                    g.fill = Color(0.0, 0.0, 1.0, 0.5)
                    boxes.boxtype[2].forEach {
                        g.strokeRect(it.x, it.y, it.width, it.height)
                        g.fillRect(it.x, it.y, it.width, it.height)
                    }
                    g.stroke = Color(0.0, 1.0, 0.0, 0.9)
                    g.fill = Color(0.0, 1.0, 0.0, 0.5)
                    boxes.boxtype[3].forEach {
                        g.strokeRect(it.x, it.y, it.width, it.height)
                        g.fillRect(it.x, it.y, it.width, it.height)
                    }
                }
                g.restore()
            }
        }

        g.restore()
    }

    private val hexArray = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}