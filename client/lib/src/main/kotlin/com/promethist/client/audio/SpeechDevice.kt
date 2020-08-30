package com.promethist.client.audio

import java.io.Closeable

interface SpeechDevice : Closeable {

    val isSpeechDetected: Boolean
    val speechAngle: Int
}