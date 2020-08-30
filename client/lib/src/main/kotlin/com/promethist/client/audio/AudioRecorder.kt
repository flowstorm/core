package com.promethist.client.audio

interface AudioRecorder {

    fun start(name: String)
    fun write(data: ByteArray)
    fun stop()
}