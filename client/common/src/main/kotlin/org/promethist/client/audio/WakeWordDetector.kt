package org.promethist.client.audio

interface WakeWordDetector {

    fun detect(buffer: ByteArray, count: Int): Boolean
}