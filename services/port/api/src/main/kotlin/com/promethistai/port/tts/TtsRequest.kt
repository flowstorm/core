package com.promethistai.port.tts

data class TtsRequest(
        var text: String? = null,
        override var language: String? = null,
        override var gender: Gender? = null,
        override var voice: String? = null) : TtsConfig()

