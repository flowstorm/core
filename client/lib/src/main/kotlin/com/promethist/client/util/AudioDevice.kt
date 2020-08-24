package com.promethist.client.util

interface AudioDevice {

    data class Format(val sampleRate: Int = 16000, val sampleSize: Int = 16, val channels: Int = 1) {

        companion object {
            var DEFAULT = Format()
        }
    }

    var callback: AudioCallback?

    /**
     * Start audio processing.
     */
    fun start()

    /**
     * Stop audio procesing.
     */
    fun stop()

    /**
     * Close audio device.
     */
    fun close(waitFor: Boolean = false)
}