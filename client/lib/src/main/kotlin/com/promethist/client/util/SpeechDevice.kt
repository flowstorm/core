package com.promethist.client.util

import java.io.Closeable

interface SpeechDevice : Closeable {

    val isSpeechDetected: Boolean
    val speechAngle: Int
}