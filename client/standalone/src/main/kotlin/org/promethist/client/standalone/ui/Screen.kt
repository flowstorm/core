package org.promethist.client.standalone.ui

import javafx.animation.Timeline
import javafx.application.Application
import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.effect.DropShadow
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCombination
import javafx.scene.layout.*
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.stage.Stage
import org.promethist.client.BotClient
import org.promethist.common.AppConfig
import java.io.InputStream
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class Screen : Application(), AnimatedImage.Callback {

    private val textBackgroundImage = Image(javaClass.getResourceAsStream("/text-bgrnd.png"))
    private val maskImage = Image(javaClass.getResourceAsStream("/mask.png"))

    private val maskView = ImageView().apply {
        image = maskImage
        isPreserveRatio = true
    }
    private val imageView = ImageView().apply {
        isPreserveRatio = true
    }
    private val mediaView = MediaView().apply {
        isPreserveRatio = true
    }
    private var imageSet = false

    private val sleepingAnimation = animation("/sleeping/frame", 50, 1500.0)
    private val respondingAnimation = animation("/responding/frame", 50, 1500.0)
    private val listeningAnimation = animation("/listening/frame", 50, 1500.0)
    private val listeningToRespondingAnimation = animation("/listening-responding/frame", 25, 750.0)
    private val respondingToListeningAnimation = animation("/responding-listening/frame", 25, 750.0)
    private val respondingToSleepingAnimation = animation("/responding-sleeping/frame", 25, 750.0)
    private val sleepingToRespondingAnimation = animation("/sleeping-responding/frame", 25, 750.0)

    private val animationQueue = LinkedList<AnimatedImage>()
    private var currentAnimation: AnimatedImage? = null

    //private val logoWhiteImage = Image(javaClass.getResourceAsStream("/logo-white.png"))
    private val textFont = Font.loadFont("https://repository.promethist.ai/fonts/AvantGardeLT-Book.TTF", 30.0)

    private val textShadow = DropShadow().apply {
        radius = 10.0
        offsetX = 5.0
        offsetY = 5.0
        color = Color.BLACK
    }
    private val userText = Text().apply {
        font = font(20.0)
        fill = Color.WHITE
        opacity = 0.3
        textAlignment = TextAlignment.LEFT
        effect = textShadow
    }
    private val botText = Text().apply {
        font = font(30.0)
        fill = Color.WHITE
        opacity = 0.7
        textAlignment = TextAlignment.CENTER
        effect = textShadow
    }

    private fun animation(path: String, count: Int, duration: Double) =
            AnimatedImage(imageView, path, count, duration, this).apply {
                cycleCount = Timeline.INDEFINITE
            }

    override fun onLastFrame() {
        if (animations && animationQueue.isNotEmpty()) {
            currentAnimation?.stop()
            currentAnimation = animationQueue.remove()
            currentAnimation?.play()
        }
    }

    private fun font(size: Double) = Font.font("AvantGarde LT Book", size)

    override fun start(stage: Stage) = with (stage) {
        val grid = GridPane().apply {
            hgap = -50.0
            ColumnConstraints().apply {
                percentWidth = 100.0
                columnConstraints.addAll(this, this)
            }
            RowConstraints().apply {
                percentHeight = 100.0
                rowConstraints.add(this)
            }
            setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE)
            val halfWidthProperty = ObservableValueProxy(stage.widthProperty() as ObservableValue<Double>) { it / 2 }
            add(StackPane().apply {
                imageView.fitWidthProperty().bind(halfWidthProperty)
                mediaView.fitWidthProperty().bind(halfWidthProperty)
                maskView.fitWidthProperty().bind(imageView.fitWidthProperty())
                children.add(imageView)
                children.add(mediaView)
                children.add(maskView)
            }, 0, 0)
            add(StackPane().apply {
                children.add(
                    ImageView().apply {
                        isPreserveRatio = true
                        fitWidthProperty().bind(halfWidthProperty)
                        image = textBackgroundImage
                    }
                )
                botText.wrappingWidthProperty().bind(halfWidthProperty)
                userText.translateYProperty().bind(ObservableValueProxy(stage.heightProperty() as ObservableValue<Double>) { -it / 4 })
                children.add(botText)
                children.add(userText)
            }, 1, 0)
        }
        heightProperty().addListener { _, _, newHeight ->
            val w = newHeight.toDouble() / 25
            userText.font = font(w)
            botText.font = font(w * 1.5)
        }
        setOnCloseRequest {
            exitProcess(0)
        }
        title = AppConfig.instance["title"]
        //fullScreenExitHint = ""
        fullScreenExitKeyCombination = KeyCombination.NO_MATCH
        scene = if (fullScreen) {
            isFullScreen = true
            isMaximized = true
            Scene(grid, background)
        } else {
            Scene(grid, 1000.0, 500.0, background)
        }
        scene.onKeyPressed = EventHandler { client?.touch() }
        scene.onMouseClicked = EventHandler { client?.touch() }
        instance = this@Screen
        animationQueue.add(sleepingAnimation)
        onLastFrame()
        show()
    }

    fun viewUserText(text: String) {
        this.userText.text = "\"$text\""
    }

    fun viewBotText(text: String) {
        this.botText.text = text
    }

    fun viewImage(input: InputStream) {
        currentAnimation?.pause()
        imageView.image = Image(input)
        imageSet = true
    }

    fun viewMedia(url: String) {
        mediaView.mediaPlayer = MediaPlayer(Media(url)).apply {
            isAutoPlay = true
            cycleCount = MediaPlayer.INDEFINITE
        }
        mediaView.mediaPlayer.play()
    }

    fun stateChange(fromState: BotClient.State, toState: BotClient.State) {
        when (toState) {
            BotClient.State.Listening -> {
                this.userText.text = ""
                if (imageSet) {
                    imageView.image
                } else {
                    when (fromState) {
                        BotClient.State.Responding -> animationQueue.add(respondingToListeningAnimation)
                    }
                    animationQueue.add(listeningAnimation)
                }
            }
            BotClient.State.Processing -> {

            }
            BotClient.State.Responding -> {
                currentAnimation?.play()
                this.botText.text = ""
                imageSet = false
                when (fromState) {
                    BotClient.State.Processing -> animationQueue.add(listeningToRespondingAnimation)
                    BotClient.State.Listening -> animationQueue.add(listeningToRespondingAnimation)
                    BotClient.State.Sleeping -> animationQueue.add(sleepingToRespondingAnimation)
                }
                animationQueue.add(respondingAnimation)
            }
            else -> {
                currentAnimation?.play()
                imageSet = false
                when (fromState) {
                    BotClient.State.Responding -> animationQueue.add(respondingToSleepingAnimation)
                }
                animationQueue.add(sleepingAnimation)
            }
        }
    }

    companion object {

        val background = Color.rgb(7, 0, 30)
        var fullScreen = false
        var animations = true
        var client: BotClient? = null
        var instance: Screen? = null

        fun launch() = launch(Screen::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            thread {
                fullScreen = false
                launch()
            }
        }
    }
}