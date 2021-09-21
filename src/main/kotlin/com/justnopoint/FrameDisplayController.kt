package com.justnopoint

import com.justnopoint.`interface`.Character
import com.justnopoint.`interface`.FrameDataProvider
import com.justnopoint.`interface`.FrameRenderer
import com.justnopoint.`interface`.Properties
import com.justnopoint.`interface`.Sequence
import com.justnopoint.matsuri.MatsuriFrameDataProvider
import com.justnopoint.util.AnimHelper
import javafx.animation.AnimationTimer
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.ComboBox
import javafx.scene.control.MenuBar
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import okio.Path.Companion.toPath
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*


class FrameDisplayController: Initializable {
    @FXML private lateinit var view: ViewerWindow
    @FXML private lateinit var menuBar: MenuBar
    @FXML private lateinit var topMenu: HBox
    @FXML private lateinit var characterSelect: ComboBox<Character>
    @FXML private lateinit var sequenceSelect: ComboBox<Sequence>
    @FXML private lateinit var toggleBoxes: CheckMenuItem
    @FXML private lateinit var toggleAxis: CheckMenuItem
    @FXML private lateinit var toggleDebug: CheckMenuItem
    @FXML private lateinit var toggleHideKnown: CheckMenuItem
    @FXML private lateinit var toggleBinds: CheckMenuItem
    @FXML private lateinit var animate: CheckMenuItem

    var frameDataProvider: FrameDataProvider? = null
    var currentFrame = 0
    var sequenceTime = 0
    var sequence: Sequence? = null
    var properties = HashMap<String, Boolean>()

    private val loadDialog by lazy {
        LoadDialog()
    }

    @FXML
    protected fun resetPosition() {
        view.resetPosition()
    }

    @FXML
    protected fun showDirectoryChooser(event: ActionEvent) {
        loadDialog.defaultPath = File(".")
        val result = loadDialog.showAndWait()
        if (result.get() == ButtonType.OK) {
            loadDialog.folder.let {
                val provider = MatsuriFrameDataProvider(it.absolutePath.toPath())
                frameDataProvider = provider
                view.rendererProvider = provider
                bindRendererProperties(provider.getFrameRenderer())
                characterSelect.items = FXCollections.observableList(provider.getCharacters())
                characterSelect.valueProperty().set(provider.currentChar)
                sequenceSelect.selectionModel.select(0)
            }

            try {
                start()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            } catch (e: InterruptedException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
    }

    private fun bindRendererProperties(renderer: FrameRenderer) {
        val props = renderer.getProperties()
        toggleBoxes.isDisable = !props.contains(Properties.BOXES)
        toggleAxis.isDisable = !props.contains(Properties.AXIS)
        toggleBinds.isDisable = !props.contains(Properties.BINDS)
        toggleDebug.isDisable = !props.contains(Properties.DEBUG)
        toggleHideKnown.isDisable = !props.contains(Properties.KNOWN)
    }

    private var keyListener: EventHandler<KeyEvent> = EventHandler { event ->
        when (event.code) {
            KeyCode.LEFT -> {
                if (currentFrame > 0)
                    currentFrame--
                animate.selectedProperty().set(false)
            }
            KeyCode.RIGHT -> {
                if (currentFrame + 1 < (sequence?.getFramecount() ?: -1)) {
                    currentFrame++
                }
                animate.selectedProperty().set(false)
            }
            KeyCode.SPACE -> animate.selectedProperty().set(!animate.selectedProperty().get())
            KeyCode.PLUS, KeyCode.ADD, KeyCode.EQUALS -> {
                if(view.currentZoom < 4) {
                    view.currentZoom++
                }
            }
            KeyCode.MINUS, KeyCode.SUBTRACT -> {
                if(view.currentZoom > 1) {
                    view.currentZoom--
                }
            }
            else -> {}
        }
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        menuBar.isFocusTraversable = true

        Platform.runLater {
            menuBar.prefWidthProperty().bind(menuBar.scene.window.widthProperty())
            val theScene = menuBar.scene
            view.widthProperty().bind(theScene.window.widthProperty())
            view.heightProperty().bind(theScene.window.heightProperty().subtract(topMenu.heightProperty()))

            theScene.addEventFilter(KeyEvent.KEY_PRESSED) { event -> keyListener.handle(event) }
            theScene.addEventFilter(MouseEvent.MOUSE_PRESSED) { event -> view.onClick(event) }
            theScene.addEventFilter(MouseEvent.MOUSE_DRAGGED) { event -> view.onDrag(event) }
            theScene.addEventFilter(MouseEvent.MOUSE_RELEASED) { event -> view.onRelease(event) }
        }

        view.prepareForRendering()

        characterSelect.itemsProperty().addListener { _, _, newValue ->
            if(!newValue.isEmpty()) {
                characterSelect.selectionModel.select(0)
            }
        }
        sequenceSelect.itemsProperty().addListener { _, _, newValue ->
            if(!newValue.isEmpty()) {
                sequenceSelect.selectionModel.select(0)
            }
        }
        characterSelect.valueProperty().addListener { _, _, newValue ->
            if(newValue != null) {
                frameDataProvider?.let { provider ->
                    provider.setSelectedCharacter(newValue)
                    sequenceSelect.items = FXCollections.observableList(provider.getSequences())
                }
            }
        }
        sequenceSelect.valueProperty().addListener { _, _, newValue ->
            sequence = newValue
            currentFrame = 0
            sequenceTime = 0
        }

        toggleBoxes.selectedProperty().addListener { _, _, bool ->
            properties[Properties.BOXES] = bool
        }
        toggleDebug.selectedProperty().addListener { _, _, bool ->
            properties[Properties.DEBUG] = bool
        }
        toggleHideKnown.selectedProperty().addListener { _, _, bool ->
            properties[Properties.KNOWN] = bool
        }
        toggleBinds.selectedProperty().addListener { _, _, bool ->
            properties[Properties.BINDS] = bool
        }
        toggleAxis.selectedProperty().addListener { _, _, bool ->
            properties[Properties.AXIS] = bool
        }
    }

    private var looper: AnimationTimer? = null
    @Throws(IOException::class, InterruptedException::class)
    fun start()  {
        view.running = false
        looper?.stop()
        looper = object: AnimationTimer() {
            var lastFrameNanos = 0L
            var framecount = 0
            var framesSkipped = 0
            var skipFrame = false

            override fun handle(now: Long) {
                framecount++
                if(animate.selectedProperty().get()) {
                    sequenceTime++
                    currentFrame = AnimHelper.getFrameForTime(sequence, sequenceTime, true)
                }

                if(sequence?.getFrames()?.isNotEmpty() == true && currentFrame >= 0) {
                    view.currentFrame = sequence?.getFrames()?.get(currentFrame)
                } else {
                    view.currentFrame = null
                }

                val currentFrameNanos = now-lastFrameNanos
                if(currentFrameNanos > FrameDisplay.frameDurationNanos) { //We're on time.
                    skipFrame=true
                }

                if(!skipFrame) {
                    view.render(properties)
                } else {
                    framesSkipped++
                    skipFrame = false
                }

                lastFrameNanos = now
            }}
        looper?.start()
    }
}