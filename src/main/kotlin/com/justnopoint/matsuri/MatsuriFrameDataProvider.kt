package com.justnopoint.matsuri

import com.justnopoint.`interface`.*
import com.justnopoint.util.AnimHelper
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.io.File
import java.io.RandomAccessFile

class MatsuriFrameDataProvider(private val matsuriHome: File): FrameDataProvider {
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
}