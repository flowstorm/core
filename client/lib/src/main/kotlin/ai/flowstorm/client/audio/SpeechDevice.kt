package ai.flowstorm.client.audio

import java.io.Closeable

interface SpeechDevice : Closeable {

    val isSpeechDetected: Boolean
    val speechAngle: Int
}