package org.promethist.client.standalone.io

import org.promethist.client.audio.SpeechDevice

object NoSpeechDevice : SpeechDevice {

    override val isSpeechDetected = true
    override val speechAngle = 0
    override fun close() {}

    override fun toString(): String = this::class.simpleName!!
}