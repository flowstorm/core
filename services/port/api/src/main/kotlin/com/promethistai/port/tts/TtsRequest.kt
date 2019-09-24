package com.promethistai.port.tts

data class TtsRequest(
        var text: String? = null,
        override var language: String? = null,
        override var gender: Gender? = null,
        override var voice: String? = null,
        override var speakingRate: Double = 1.0,
        override var pitch: Double = 0.0,
        override var volumeGain: Double = 0.0) : TtsConfig()

