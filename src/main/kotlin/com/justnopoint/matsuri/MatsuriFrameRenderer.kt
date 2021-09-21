package com.justnopoint.matsuri

import com.justnopoint.FrameDisplay
import com.justnopoint.`interface`.*
import com.justnopoint.util.AnimHelper
import com.justnopoint.util.getShortAt
import okio.FileHandle
import java.text.DecimalFormat

class MatsuriFrameRenderer(charFile: FileHandle, effFile: FileHandle?, val hanyou: ImagFile): FrameRenderer {
    private val grapFile: GrapFile
    private val frameFile: AnimFile
    val animFile: GanmFile
    private val grecFile: GrecFile
    private val thwpFile: ThwpFile
    private val gatkFile: GatkFile
    private val eff = ImagFile()

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

    override fun getProperties(): List<String> {
        return listOf(FrameDisplay.BOXES, FrameDisplay.DEBUG, FrameDisplay.AXIS, FrameDisplay.KNOWN, FrameDisplay.BINDS)
    }

    override fun getRenderableSprites(frame: Frame, props: HashMap<String, Boolean>): List<RenderableSprite> {
        if (frame !is MatsuriFrameDataProvider.MatsuriFrame) {
            return emptyList()
        }

        val sprites = mutableListOf<RenderableSprite>()
        val name = "${animFile.prefix}${frame.ganmFrame.frame}"
        if(props[FrameDisplay.BINDS] != false) {
            frame.ganmFrame.getBoundEnemy()?.let {
                val bindName = "${animFile.prefix}${it.frame}"
                frameFile.getAnimFrame(bindName)?.let { bindFrame ->
                    grapFile.getSprite(bindFrame.refs[0].ref1)?.let { sprite ->
                        val anchor = thwpFile.getCoords(it.frame - 9000)[it.anchorPoint]
                        val frameAxisX = (frameFile.getXAxis(bindFrame.refs[0])).toInt()
                        val frameAxisY = (frameFile.getYAxis(bindFrame.refs[0])).toInt()
                        sprites.add(
                            RenderableSprite(
                                raster = sprite.raster,
                                width = sprite.width,
                                height = sprite.height,
                                axisX = it.axisx - (frameAxisX - anchor.first),
                                axisY = it.axisy + (frameAxisY - anchor.second),
                                scaleX = -1.0))
                    } ?: println("Missing sprite reference ${bindFrame.refs[0].ref1}")
                }
            }
        }
        frameFile.getAnimFrame(name)?.let { animFrame ->
            for (ref in animFrame.refs.reversed()) {
                grapFile.getSprite(ref.ref1)?.let { sprite ->
                    val frameAxisX = (frameFile.getXAxis(ref))
                    val frameAxisY = (frameFile.getYAxis(ref))
                    sprites.add(
                        RenderableSprite(
                            raster = sprite.raster,
                            width = sprite.width,
                            height = sprite.height,
                            axisX = frameAxisX.toInt(),
                            axisY = frameAxisY.toInt(),
                            rotation = frameFile.getRotation(ref).toDouble(),
                            scaleX = (frameFile.getXScale(ref)).toDouble(),
                            scaleY = (frameFile.getYScale(ref)).toDouble(),
                            opacity = frameFile.getOpacity(ref)))
                } ?: println("Missing sprite reference ${ref.ref1}")
            }
        }
        frame.ganmFrame.getEffects().reversed().forEach { effect ->
            val img = effect.toSprite(eff, hanyou)
            val scale = frame.ganmFrame.getEffectScale(effect)
            val rotation = frame.ganmFrame.getEffectRotation(effect)
            sprites.add(
                RenderableSprite(
                raster = img.raster,
                    width = img.width,
                    height = img.height,
                    axisX = (effect.axisx * scale.first).toInt(),
                    axisY = (effect.axisy * scale.second).toInt(),
                    rotation = rotation,
                    scaleX = scale.first,
                    scaleY = scale.second
            ))
        }
        return sprites
    }

    override fun getRenderableText(frame: Frame, props: HashMap<String, Boolean>): List<RenderableText> {
        if (frame !is MatsuriFrameDataProvider.MatsuriFrame) {
            return emptyList()
        }

        val renderables = mutableListOf<RenderableText>()

        val frameTime = AnimHelper.getTimeForFrame(frame.seq, frame.seq.ganmFrames.indexOf(frame.ganmFrame))
        val totalAnimTime = AnimHelper.getSequenceDurationTotal(frame.seq)
        renderables.add(RenderableText("Time $frameTime/$totalAnimTime", 20, 20))
        renderables.add(RenderableText("Duration ${frame.getDuration()}", 20, 40))

        frame.ganmFrame.props[GanmFile.HITDEF]?.getShortAt(0)?.let(gatkFile::getHitdef)?.let { hitdef ->
            if(props[FrameDisplay.DEBUG] != false) {
                renderables.add(RenderableText("Hitdef ${bytesToHex(hitdef.data)}", 20, 100))
            }
            renderables.add(RenderableText("Damage ${hitdef.getDamage()}", 110, 20))
            val plusMinusNF = DecimalFormat("+#;-#")
            val remaining = totalAnimTime - frameTime
            val hitTime = animFile.getAnim(hitdef.getHitAnim()).sumOf { it.duration }

            renderables.add(RenderableText("Guard | L | H | A |", 110, 40))
            renderables.add(RenderableText("      | ${if(hitdef.isLowUnblockable()) 'X' else 'O'} | ${if(hitdef.isHighUnblockable()) 'X' else 'O'} | ${if(hitdef.isAirUnblockable()) 'X' else 'O'} |", 110, 60))
            renderables.add(RenderableText("On Hit ${plusMinusNF.format(hitTime - remaining)}", 110, 80))
        }

        val propslist = mutableListOf<String>()
        frame.ganmFrame.getHelperSpawn()?.let {
            propslist.add("Spawn Entity ${it.ref} at ${it.x.toDouble()},${-it.y.toDouble()}")
        }

        frame.ganmFrame.getArmor()?.let {
            if(it) "Armor Enabled" else "Armor Disabled"
        }?.run(propslist::add)

        frame.ganmFrame.getCancel()?.let {
            if(it) "Cancel Window" else "Cancelling Disabled"
        }?.run(propslist::add)

        frame.ganmFrame.getInvulnerability()?.let {
            if(it) "Invulnerable" else "Invuln Disabled"
        }?.run(propslist::add)

        frame.ganmFrame.getGravity()?.let {
            "Gravity Enabled"
        }?.run(propslist::add)

        frame.ganmFrame.getSound()?.let {
            "Play Sound ${bytesToHex(it)}"
        }?.run(propslist::add)

        propslist.forEachIndexed { index, string ->
            renderables.add(RenderableText(string, 260, 20 + 20 * index))
        }

        val name = "${animFile.prefix}${frame.ganmFrame.frame}"
        frameFile.getAnimFrame(name)?.let { data ->
            if (props[FrameDisplay.DEBUG] != false) {
                renderables.add(RenderableText("$name ${bytesToHex(data.head)}", 20, 350))
                var posy = 380
                for (ref in data.refs) {
                    var posx = 240
                    renderables.add(RenderableText("${ref.ref1} ${ref.ref2} ${bytesToHex(ref.head)}", 20, posy))
                    ref.props.entries.forEach prop@{ (key, value) ->
                        if ((props[FrameDisplay.KNOWN] != true) && listOf(
                                AnimFile.ROT,
                                AnimFile.SCALE,
                                AnimFile.AXIS,
                                AnimFile.RGBA_ADJUST
                            ).contains(key)
                        ) {
                            return@prop
                        }
                        renderables.add(RenderableText("${key}-${bytesToHex(value)}", posx, posy))
                        posx += (60 + 14 * value.size)
                    }
                    posy += 25
                }
            }
        }

        return renderables
    }

    override fun getRenderableBoxes(frame: Frame, props: HashMap<String, Boolean>): List<RenderableBox> {
        if (frame !is MatsuriFrameDataProvider.MatsuriFrame) {
            return emptyList()
        }

        val renderables = mutableListOf<RenderableBox>()

        if (props[FrameDisplay.AXIS] != false) {
            renderables.add(
                RenderableBox(-2, 0, 5, 1, Triple(0.0, 1.0, 0.0)))
            renderables.add(
                RenderableBox(0, -2, 1, 5, Triple(0.0, 1.0, 0.0)))

            if (frame.ganmFrame.frame >= 9000) {
                thwpFile.getCoords(frame.ganmFrame.frame - 9000).forEachIndexed { _, coords ->
                    renderables.add(
                        RenderableBox(coords.first -2, coords.second, 5, 1, Triple(1.0, 0.0, 1.0)))
                    renderables.add(
                        RenderableBox(coords.first, coords.second -2, 1, 5, Triple(1.0, 0.0, 1.0)))
                }
            }
        }

        if (props[FrameDisplay.BOXES] != false) {
            grecFile.getBoxes(frame.ganmFrame.frame)?.let { boxes ->
                boxes.boxtype[0].forEach {
                    renderables.add(RenderableBox(it.x, it.y, it.width, it.height, Triple(1.0, 1.0, 1.0)))
                }
                boxes.boxtype[1].forEach {
                    renderables.add(RenderableBox(it.x, it.y, it.width, it.height, Triple(1.0, 0.0, 0.0)))
                }
                boxes.boxtype[2].forEach {
                    renderables.add(RenderableBox(it.x, it.y, it.width, it.height, Triple(0.0, 0.0, 1.0)))
                }
                boxes.boxtype[3].forEach {
                    renderables.add(RenderableBox(it.x, it.y, it.width, it.height, Triple(0.0, 1.0, 0.0)))
                }
            }
        }

        return renderables
    }

    private val hexArray = "0123456789ABCDEF".toCharArray()
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}