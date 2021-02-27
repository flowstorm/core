package ai.flowstorm.client.standalone.io

import ai.flowstorm.client.audio.SpeechDevice

object NoSpeechDevice : SpeechDevice {

    override val isSpeechDetected = true
    override val speechAngle = 0
    override fun close() {}

    override fun toString(): String = this::class.simpleName!!
}