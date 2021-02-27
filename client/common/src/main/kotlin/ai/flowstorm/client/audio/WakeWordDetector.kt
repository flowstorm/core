package ai.flowstorm.client.audio

interface WakeWordDetector {

    fun detect(buffer: ByteArray, count: Int): Boolean
}