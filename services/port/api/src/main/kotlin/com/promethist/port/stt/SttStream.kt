package com.promethist.port.stt

interface SttStream: AutoCloseable {

    fun write(data: ByteArray, offset: Int, size: Int)

}