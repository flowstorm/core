package com.promethistai.port.stt

interface SttService: AutoCloseable {

    fun createStream(): SttStream

}