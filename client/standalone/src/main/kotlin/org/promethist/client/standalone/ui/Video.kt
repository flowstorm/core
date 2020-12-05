package org.promethist.client.standalone.ui

import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.DoubleProperty
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.scene.paint.Color
import javafx.stage.Stage
import java.io.File


class Video : Application() {
    private val Dir = System.getProperty("user.dir")

    @Throws(Exception::class)
    override fun start(stage: Stage) {

        //goes to user Directory
        val f = File(Dir, "Epic Lightsaber Duel - Star Wars_ The Force Awakens.mp4")


        //Converts media to string URL
        val media = Media("https://repository.promethist.ai/media/sample.mp4")
        val player = MediaPlayer(media)
        val viewer = MediaView(player)

        //change width and height to fit video
        val width: DoubleProperty = viewer.fitWidthProperty()
        val height: DoubleProperty = viewer.fitHeightProperty()
        width.bind(Bindings.selectDouble(viewer.sceneProperty(), "width"))
        height.bind(Bindings.selectDouble(viewer.sceneProperty(), "height"))
        viewer.isPreserveRatio = true
        val root = StackPane()
        root.children.add(viewer)

        //set the Scene
        val scenes = Scene(root, 500.0, 500.0, Color.BLACK)
        stage.scene = scenes
        stage.setTitle("Riddle Game")
        stage.setFullScreen(true)
        stage.show()
        player.play()
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Video::class.java)
        }
    }
}