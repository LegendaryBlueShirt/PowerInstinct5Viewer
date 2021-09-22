package com.justnopoint

import com.justnopoint.`interface`.ImageBank
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.stage.Stage
import javafx.scene.layout.VBox
import javafx.scene.Scene
import org.koin.core.context.startKoin
import org.koin.dsl.binds
import org.koin.dsl.module

class FrameDisplay: Application() {
    companion object {
        const val FPS = 60
        const val frameDurationNanos = (1000000000.0 / FPS).toLong()
    }

    val imageModule = module {
        single { PNGBank() }.binds(arrayOf(PNGBank::class, ImageBank::class))
    }

    override fun start(primaryStage: Stage) {
        startKoin {
            modules(listOf(imageModule))
        }
        primaryStage.title = "Power Instinct 5 Framedisplay"
        val layout: VBox = FXMLLoader.load(javaClass.getResource("/FrameDisplay.fxml"))
        primaryStage.scene = Scene(layout)
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(FrameDisplay::class.java, *args)
}