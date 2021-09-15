package com.justnopoint.matsuri

import com.justnopoint.`interface`.*
import com.justnopoint.matsuri.AnimFile.Companion.AXIS
import com.justnopoint.matsuri.AnimFile.Companion.ROT
import com.justnopoint.matsuri.AnimFile.Companion.SCALE
import com.justnopoint.util.AnimHelper
import com.justnopoint.util.getShortAt
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import java.io.File
import java.io.RandomAccessFile
import java.text.DecimalFormat

class MatsuriFrameDataProvider(val matsuriHome: File): FrameDataProvider {
    private val characters = listOf(
        MatsuriCharacter("Annie Hamilton", "ah"),
        MatsuriCharacter("Angela Belti", "an"),
        MatsuriCharacter("Buntaro Kuno", "ba"),
        MatsuriCharacter("Shintaro Kuno", "bb"),
        MatsuriCharacter("Olof Linderoth", "bg"),
        //MatsuriCharacter("Shinjuro Goketsuji", "bs"),
        MatsuriCharacter("Elizabeth Belti", "eb"),
        MatsuriCharacter("Otane Goketsuji", "gt"),
        MatsuriCharacter("Oume Goketsuji", "gu"),
        MatsuriCharacter("Clara Hananokoji", "hk"),
        MatsuriCharacter("Saizo Hattori", "hs"),
        MatsuriCharacter("Takumi Hattori", "ht"),
        MatsuriCharacter("Hikaru Jomon", "js"),
        MatsuriCharacter("Kanji Kokuin", "kk"),
        MatsuriCharacter("Kanji Kokuin (Powered Up)", "km"),
        MatsuriCharacter("Kintaro Kokuin", "kn"),
        MatsuriCharacter("Keith Wayne", "kw"),
        MatsuriCharacter("Rin Oyama", "ol"),
        MatsuriCharacter("Reiji Oyama", "or"),
        MatsuriCharacter("Prince", "pc"),
        MatsuriCharacter("Poochy", "po"),
        MatsuriCharacter("Sissy", "pr"),
        MatsuriCharacter("Maruta", "sc"),
        MatsuriCharacter("Sandra Belti", "sd"),
        MatsuriCharacter("Elizabeth Belti (Young)", "se"),
        MatsuriCharacter("Super Clara", "sk"),
        MatsuriCharacter("Prince (Handsome)", "sp"),
        MatsuriCharacter("Sandra Belti (Young)", "ss"),
        MatsuriCharacter("Otane Goketsuji (Young)", "st"),
        MatsuriCharacter("Oume Goketsuji (Young)", "su"),
        MatsuriCharacter("Chinnen", "tn"),
        MatsuriCharacter("White Buffalo", "wb")
    )

    override fun getCharacters(): List<Character> {
        return characters
    }

    lateinit var renderer: MatsuriFrameRenderer

    var showBoxes = SimpleBooleanProperty(this, "showBoxes", true)
    var showDebug = SimpleBooleanProperty(this, "showDebug", true)
    var hideKnown = SimpleBooleanProperty(this, "hideKnown", false)
    var showAxis = SimpleBooleanProperty(this, "showAxis", true)
    var currentChar = SimpleObjectProperty<Character>(this, "character", characters[0])
    var palette = SimpleIntegerProperty(this, "palette", 0)
    var sequences = SimpleListProperty<Sequence>(this, "sequences")

    init {
        palette.addListener { _ ->
            loadCharacter(currentChar.get())
        }
        currentChar.addListener { _, _, newValue ->
            loadCharacter(newValue)
        }

        loadCharacter(characters[0])
    }

    override fun getCharacterSelection(): Property<Character> {
        return currentChar
    }

    override fun getObservableProperty(name: String): Property<Boolean>? {
        return when(name) {
            "showBoxes" -> showBoxes
            "showDebug" -> showDebug
            "showAxis" -> showAxis
            "hideKnown" -> hideKnown
            else -> null
        }
    }

    private fun loadCharacter(character: Character) {
        if(character !is MatsuriCharacter) {
            return
        }

        val charaFolder = File(matsuriHome, "chara")
        val charaFile = File(charaFolder, character.getFile(palette.get()))
        val raf = RandomAccessFile(charaFile, "r")

        val effectFolder = File(matsuriHome, "effect")
        val effectFile = File(effectFolder, character.getEffectFile())
        val effraf = if(effectFile.exists()) RandomAccessFile(effectFile, "r") else null

        renderer = MatsuriFrameRenderer(raf, effraf)
        sequences.value = FXCollections.observableList(renderer.animFile.offsets.entries.distinctBy { it.value }.map { MatsuriSequence(it.key, renderer.animFile.getAnim(it.key)) })
    }

    override fun getSequences(): Property<ObservableList<Sequence>> {
        return sequences
    }

    override fun getFrameRenderer(): FrameRenderer {
        return renderer
    }

    inner class MatsuriFrameRenderer(charFile: RandomAccessFile, effFile: RandomAccessFile?): FrameRenderer {
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

        override fun renderFrame(g: GraphicsContext, frame: Frame, axisX: Int, axisY: Int, zoom: Double) {
            if (frame !is MatsuriFrame) {
                return
            }
            g.save()

            g.translate(axisX.toDouble(), axisY.toDouble())
            g.scale(zoom, zoom)

            val name = "${animFile.prefix}${frame.ganmFrame.frame}"
            frameFile.getAnimFrame(name)?.let { animFrame ->
                frame.ganmFrame.getBoundEnemy()?.let {
                    val bindName = "${animFile.prefix}${it.frame}"
                    frameFile.getAnimFrame(bindName)?.let { bindFrame ->
                        grapFile.getSprite(bindFrame.refs[0].ref1)?.let { sprite ->
                            val anchor = thwpFile.getCoords(it.frame - 9000)[it.anchorPoint]
                            val frameAxisX = (frameFile.getXAxis(bindFrame.refs[0]))
                            val frameAxisY = (frameFile.getYAxis(bindFrame.refs[0]))
                            println("${anchor.first},${anchor.second}  ${it.axisx},${it.axisy}  $frameAxisX,$frameAxisY")
                            g.save()
                            g.translate(it.axisx.toDouble() - (frameAxisX-anchor.first), it.axisy.toDouble() + (frameAxisY-anchor.second))
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
            if (frame !is MatsuriFrame) {
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
                            if (hideKnown.get() && listOf(ROT, SCALE, AXIS).contains(key)) {
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
    }

    class MatsuriCharacter(val fullname: String, val tag: String): Character() {
        override fun getFullName(): String {
            return fullname
        }

        fun getFile(palette: Int): String {
            return "${tag}${palette}.vsa"
        }

        fun getEffectFile(): String {
            return "${tag}.vsa"
        }
    }

    class MatsuriSequence(val index: Int, val ganmFrames: List<GanmFrame>): Sequence() {
        companion object {
            val labels = HashMap<Int, String>().apply {
                put(0, "Standing")
                put(1, "S > C")
                put(2, "Crouching")
                put(3, "Walk F")
                put(4, "Walk B")
                put(5, "Turn S")
                put(6, "Turn C")
                put(7, "Jump Start")
                put(8, "Jump")
                put(9, "Jump Fall")
                put(10, "Jump Forward")
                put(11, "Jump Back")
                put(13, "Jump Land Neutral")
                put(14, "Jump Land Neutral")
                put(15, "Jump Land Turn")
                put(16, "Guard A Start")
                put(17, "Guard A")
                put(18, "Guard A End")
                put(19, "Guard S Light Start")
                put(20, "Guard S Light")
                put(21, "Guard S Light End")
                put(22, "Guard S Heavy Start")
                put(23, "Guard S Heavy")
                put(24, "Guard S Heavy End")
                put(25, "Guard C Light Start")
                put(26, "Guard C Light")
                put(27, "Guard C Light End")
                put(28, "Guard C Heavy Start")
                put(29, "Guard C Heavy")
                put(30, "Guard C Heavy End")
                put(31, "Run")
                put(33, "Backdash")
                put(36, "5A (close)")
                put(38, "5A (far)")
                put(39, "5C (close)")
                put(40, "5C (far)")
                put(41, "5B (close)")
                put(42, "5B (far)")
                put(43, "5D (close)")
                put(44, "5D (far)")
                put(45, "2A")
                put(46, "2C")
                put(47, "2B")
                put(48, "2D")
                put(49, "8j.C")
                put(50, "8j.D")
                put(51, "j.A")
                put(52, "j.C")
                put(53, "j.B")
                put(54, "j.D")
                put(56, "Running 5C")
                put(57, "Running 5D")
                put(73, "Grab Attempt")
                put(74, "Grab Success")
                put(129, "Intro")
                put(130, "Intro")
                put(131, "Intro")
                put(134, "Win")
                put(135, "Lose")
                put(141, "Hit High Lightest")
                put(145, "Hit High Lightest?")
                put(147, "Hit High Lightest??") //0
                put(148, "Hit High Light")  //2
                put(149, "Hit High Medium") //4
                put(150, "Hit High Heavy") //6
                put(151, "Hit Low Lightest")
                put(155, "Hit Crouch Lightest")
                put(159, "Hit Air Lightest")
                put(161, "Knocked off feet")
                put(175, "Roll Fwd")
                put(176, "Roll Fwd End")
                put(177, "Roll Back")
                put(178, "Roll Back End")
                put(179, "Throw Reject")
                put(222, "5C+D")
                put(223, "2C+D")
            }
        }

        override fun getName(): String {
            return "$index ${labels[index]?:""}"
        }

        override fun getFramecount(): Int {
            return ganmFrames.size
        }

        override fun getFrames(): List<Frame> {
            return ganmFrames.mapIndexed { i, frame -> MatsuriFrame(this, i, frame) }
        }

        override fun getLoopPoint(): Int {
            return ganmFrames.indexOfFirst { it.props.containsKey(2) }
        }
    }

    class MatsuriFrame(val seq: MatsuriSequence, val index: Int, val ganmFrame: GanmFrame): Frame {
        override fun getSequence(): Sequence {
            return seq
        }

        override fun getStartTime(): Int {
            return AnimHelper.getTimeForFrame(seq, index)
        }

        override fun getDuration(): Int {
            return ganmFrame.duration
        }
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