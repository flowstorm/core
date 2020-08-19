package com.promethist.client.standalone.io

object SpeechDeviceFactory {

    fun getSpeechDevice(name: String) = when (name) {
        "respeaker2" -> RespeakerMicArrayV2
        else -> NoSpeechDevice
    }
}