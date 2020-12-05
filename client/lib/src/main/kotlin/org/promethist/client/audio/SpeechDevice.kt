package org.promethist.client.audio

import java.io.Closeable

interface SpeechDevice : Closeable {

    val isSpeechDetected: Boolean
    val speechAngle: Int
}