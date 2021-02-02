package org.promethist.client.audio

import org.promethist.util.LoggerDelegate

abstract class InputAudioDevice(val speechDevice: SpeechDevice = NoSpeechDevice) : AudioDevice, Runnable {

    private val logger by LoggerDelegate()
    var started = false
        private set
    var closed = false
        private set
    private var close = false
    override var callback: AudioCallback? = null
        get() = field
        set(value) { field = value }
    private val buffer = ByteArray(100000)

    override fun start() {
        logger.info("Starting")
        started = true
    }

    override fun stop() {
        logger.info("Stopping")
        started = false
    }

    abstract fun read(buffer: ByteArray): Int

    override fun close(waitFor: Boolean) {
        logger.info("Closing (waitFor=$waitFor)")
        started = false
        close = true
        if (waitFor)
            while (!closed)
                Thread.sleep(50)
    }

    override fun run() {
        logger.info("Running")
        try {
            while (!close) {
                if (started) {
                    callback!!.onStart()
                    while (started) {
                        val count = read(buffer)
                        if (count > 0) {
                            //if (!speechDevice.isSpeechDetected) buffer.fill(0, 0, count - 1)
                            //else println("INPUT $count")
                            callback!!.onData(buffer, count)
                        }
                    }
                    callback!!.onStop()
                } else {
                    Thread.sleep(50)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            stop()
        }
        closed = true
    }
}