package com.justnopoint

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.stage.Stage
import javafx.scene.layout.VBox
import javafx.scene.Scene

class FrameDisplay: Application() {
    companion object {
        const val FPS = 60
        const val frameDurationNanos = (1000000000.0 / FPS).toLong()
    }

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Power Instinct 5 Framedisplay"
        val layout: VBox = FXMLLoader.load(javaClass.getResource("/FrameDisplay.fxml"))
        primaryStage.scene = Scene(layout)
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(FrameDisplay::class.java, *args)
}