package com.justnopoint

import com.justnopoint.`interface`.Character
import com.justnopoint.`interface`.FrameDataProvider
import com.justnopoint.`interface`.Sequence
import com.justnopoint.matsuri.MatsuriFrameDataProvider
import com.justnopoint.util.*
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.stage.Stage
import javafx.scene.layout.VBox
import javafx.scene.layout.Pane
import javafx.scene.layout.HBox
import javafx.scene.layout.BorderPane
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import java.io.*

class FrameDisplay: Application() {
    private lateinit var view: ViewerWindow

    var frameDataProvider: FrameDataProvider? = null

    var currentFrame = 0
    val animating = SimpleBooleanProperty(this, "animating", false)
    var sequenceTime = 0
    var menu: MenuBar? = null
    var sequence: Sequence? = null
    val characterSelect: ComboBox<Character> = ComboBox()
    val sequenceSelect: ComboBox<Sequence> = ComboBox()

    lateinit var toggleBoxes: CheckMenuItem
    lateinit var toggleAxis: CheckMenuItem
    lateinit var toggleDebug: CheckMenuItem
    lateinit var toggleHideKnown: CheckMenuItem
    lateinit var toggleBinds: CheckMenuItem

    companion object {
        const val FPS = 60
        const val frameDurationNanos = (1000000000.0/FPS).toLong()
    }

    private var keyListener: EventHandler<KeyEvent> = EventHandler { event ->
        when (event.code) {
            KeyCode.LEFT -> {
                if (currentFrame > 0)
                    currentFrame--
                animating.set(false)
            }
            KeyCode.RIGHT -> {
                if (currentFrame + 1 < (sequence?.getFramecount() ?: -1)) {
                    currentFrame++
                }
                animating.set(false)
            }
            KeyCode.SPACE -> animating.value = !animating.value
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

    override fun start(primaryStage: Stage) {
        view = ViewerWindow()
        primaryStage.onCloseRequest = view.getWindowCloseHandler()
        primaryStage.title = "Power Instinct 5 Framedisplay"

        val theScene = Scene(VBox(), 800.0, 600.0)

        theScene.addEventFilter(KeyEvent.KEY_PRESSED) { event -> keyListener.handle(event) }

        createMenu(view)
        menu?.prefWidthProperty()?.bind(primaryStage.widthProperty())

        val border = BorderPane()
        val topMenu = HBox()

        topMenu.children.addAll(characterSelect, sequenceSelect)
        // setup our canvas size and put it into the content of the frame
        border.top = topMenu

        val pane = Pane()
        border.center = pane

        pane.children.add(view)

        view.widthProperty().bind(primaryStage.widthProperty())
        view.heightProperty().bind(primaryStage.heightProperty().subtract(topMenu.heightProperty()))

        theScene.addEventFilter(MouseEvent.MOUSE_PRESSED) { event -> view.onClick(event) }
        theScene.addEventFilter(MouseEvent.MOUSE_DRAGGED) { event -> view.onDrag(event) }
        theScene.addEventFilter(MouseEvent.MOUSE_RELEASED) { event -> view.onRelease(event) }

        (theScene.root as VBox).children.addAll(menu, border)

        view.prepareForRendering()

        primaryStage.scene = theScene
        primaryStage.show()
    }

    private val loadDialog by lazy {
        LoadDialog()
    }
    private fun showDirectoryChooser() {
        loadDialog.defaultPath = File(".")
        val result = loadDialog.showAndWait()
        if (result.get() == ButtonType.OK) {
            loadDialog.folder.let {
                frameDataProvider?.let { frameData ->
                    characterSelect.valueProperty().unbindBidirectional(frameData.getCharacterSelection())
                    sequenceSelect.itemsProperty().unbind()
                }
                frameDataProvider = MatsuriFrameDataProvider(it)
                frameDataProvider?.let { frameData ->
                    characterSelect.items = FXCollections.observableList(frameData.getCharacters())
                    characterSelect.valueProperty().bindBidirectional(frameData.getCharacterSelection())
                    sequenceSelect.itemsProperty().bind(frameData.getSequences())

                    frameData.getFrameRenderer().getObservableProperty("showBoxes")?.let { bool ->
                        toggleBoxes.selectedProperty().bindBidirectional(bool)
                        toggleBoxes.isDisable = false
                    }
                    frameData.getFrameRenderer().getObservableProperty("showAxis")?.let { bool ->
                        toggleAxis.selectedProperty().bindBidirectional(bool)
                        toggleAxis.isDisable = false
                    }
                    frameData.getFrameRenderer().getObservableProperty("showDebug")?.let { bool ->
                        toggleDebug.selectedProperty().bindBidirectional(bool)
                        toggleDebug.isDisable = false
                    }
                    frameData.getFrameRenderer().getObservableProperty("hideKnown")?.let { bool ->
                        toggleHideKnown.selectedProperty().bindBidirectional(bool)
                        toggleHideKnown.isDisable = false
                    }
                    frameData.getFrameRenderer().getObservableProperty("showBinds")?.let { bool ->
                        toggleBinds.selectedProperty().bindBidirectional(bool)
                        toggleBinds.isDisable = false
                    }

                    view.rendererProvider = frameData
                }
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

    private fun createMenu(view: ViewerWindow) {
        menu = MenuBar().apply {

            val fileMenu = Menu("File")
            val loadDirectory = MenuItem("Load Directory")
            loadDirectory.setOnAction { showDirectoryChooser() }
            fileMenu.items.add(loadDirectory)

            val viewMenu = Menu("View")
            val resetPosition = MenuItem("Reset Position")
            resetPosition.setOnAction { view.resetPosition() }
            viewMenu.items.add(resetPosition)

            val toggleAnimation = CheckMenuItem("Animate")
            toggleAnimation.selectedProperty().bindBidirectional(animating)
            viewMenu.items.add(toggleAnimation)

            toggleBoxes = CheckMenuItem("Show Boxes")
            toggleBoxes.isDisable = true
            viewMenu.items.add(toggleBoxes)

            toggleAxis = CheckMenuItem("Show Axis")
            toggleAxis.isDisable = true
            viewMenu.items.add(toggleAxis)

            toggleDebug = CheckMenuItem("Show Debug Information")
            toggleDebug.isDisable = true
            viewMenu.items.add(toggleDebug)

            toggleHideKnown = CheckMenuItem("Hide Known Debug Values")
            toggleHideKnown.isDisable = true
            viewMenu.items.add(toggleHideKnown)

            toggleBinds = CheckMenuItem("Show Bound Targets In Throws")
            toggleBinds.isDisable = true
            viewMenu.items.add(toggleBinds)

            menus.addAll(fileMenu, viewMenu)

            characterSelect.itemsProperty().addListener { _, _, newValue ->
                characterSelect.isDisable = newValue.isEmpty()
            }
            sequenceSelect.valueProperty().addListener(ChangeListener<Sequence> { _, _, newValue ->
                if (newValue == null)
                    return@ChangeListener
                if (sequenceSelect.isDisabled)
                    return@ChangeListener
                sequenceChange(newValue)
            })
            sequenceSelect.itemsProperty().addListener { _, _, newValue ->
                if(newValue.isEmpty()) {
                    sequenceSelect.isDisable = true
                } else {
                    sequenceSelect.isDisable = false
                    sequenceSelect.selectionModel.select(0)
                }
            }

            characterSelect.isDisable = true
            sequenceSelect.isDisable = true
        }
    }

    private fun sequenceChange(newSequence: Sequence) {
        sequence = newSequence
        currentFrame = 0
        sequenceTime = 0
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
				if(animating.get()) {
					sequenceTime++
					currentFrame = AnimHelper.getFrameForTime(sequence, sequenceTime, true)
				}

                if(sequence?.getFrames()?.isNotEmpty() == true && currentFrame >= 0) {
                    view.currentFrame = sequence?.getFrames()?.get(currentFrame)
                } else {
                    view.currentFrame = null
                }

				val currentFrameNanos = now-lastFrameNanos
				if(currentFrameNanos > frameDurationNanos) { //We're on time.
					skipFrame=true
				}

				if(!skipFrame) {
					view.render()
				} else {
					framesSkipped++
					skipFrame = false
				}

				lastFrameNanos = now
			}}
		looper?.start()
	}
}

fun main(args: Array<String>) {
    Application.launch(FrameDisplay::class.java, *args)
}