package com.promethist.client.util

import com.promethist.util.LoggerDelegate

abstract class InputAudioDevice(val format: AudioDevice.Format = AudioDevice.Format.DEFAULT) : AudioDevice, Runnable {

    private val logger by LoggerDelegate()
    val limit = 1024 * 128 // audio capture limit in bytes
    var started = false
        private set
    var closed = false
        private set
    private var close = false
    override var callback: AudioCallback? = null
        get() = field
        set(value) { field = value }

    override fun start() {
        logger.info("start()")
        started = true
    }

    override fun stop() {
        logger.info("stop()")
        started = false
    }

    abstract fun read(buffer: ByteArray): Int

    override fun close(waitFor: Boolean) {
        logger.info("close(waitFor = $waitFor)")
        started = false
        close = true
        if (waitFor)
            while (!closed)
                Thread.sleep(50)
    }

    override fun run() {
        logger.info("run()")
        try {
            while (!close) {
                if (started) {
                    callback!!.onStart()
                    val buffer = ByteArray(bufferSize)
                    while (started/* && totalCount < limit*/) {
                        val count = read(buffer)
                        if (count > 0) {
                            callback!!.onData(buffer, count)
                            //totalCount += count
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