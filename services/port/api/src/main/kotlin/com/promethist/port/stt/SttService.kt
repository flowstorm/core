package com.promethist.port.stt

interface SttService: AutoCloseable {

    fun createStream(): SttStream

}