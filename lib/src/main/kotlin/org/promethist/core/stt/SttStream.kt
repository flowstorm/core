package org.promethist.core.stt

interface SttStream: AutoCloseable {

    fun write(data: ByteArray, offset: Int, size: Int)

}