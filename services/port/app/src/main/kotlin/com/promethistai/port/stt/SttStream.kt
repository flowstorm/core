package com.promethistai.port.stt

interface SttStream: AutoCloseable {

    fun write(data: ByteArray, offset: Int, size: Int)

}