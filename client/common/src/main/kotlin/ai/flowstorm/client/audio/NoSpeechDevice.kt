package ai.flowstorm.client.audio

object NoSpeechDevice : SpeechDevice {
    override val isSpeechDetected = false
    override val speechAngle = -1
    override fun close() {}
}