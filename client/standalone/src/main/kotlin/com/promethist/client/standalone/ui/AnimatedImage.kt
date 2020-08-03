package com.promethist.client.standalone.ui

import javafx.animation.Interpolator
import javafx.animation.Transition
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.util.Duration
import kotlin.math.floor
import kotlin.math.min

open class AnimatedImage(private val imageView: ImageView, private val path: String, count: Int, duration: Double, private val callback: Callback) : Transition() {

    interface Callback {
        fun onLastFrame()
    }

    private val sequence = getImageSequence(path, count)
    private var lastIndex = 0
    private val maxIndex = sequence.size - 1

    init {
        imageView.image = sequence[0]
        cycleCount = 1
        cycleDuration = Duration.millis(duration)
        interpolator = Interpolator.LINEAR
    }

    override fun interpolate(k: Double) {
        val index = min(floor(k * sequence.size).toInt(), maxIndex)
        if (index != lastIndex) {
            imageView.image = sequence[index]
            lastIndex = index
            if (index == maxIndex) {
                callback.onLastFrame()
            }
        }
    }

    override fun toString() = "${this::class.simpleName}(path = $path)"

    companion object {

        fun getImageSequence(path: String, count: Int, ext: String = "png"): List<Image> {
            val sequence = mutableListOf<Image>()
            for (i in 1..count) {
                val name = path + "%04d".format(i) + "." + ext
                sequence.add(Image(javaClass.getResourceAsStream(name)))
            }
            return sequence
        }


    }
}