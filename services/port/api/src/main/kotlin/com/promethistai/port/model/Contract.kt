package com.promethistai.port.model

import com.promethistai.port.tts.TtsConfig

data class Contract(
    var name: String? = null,
    var appKey: String? = null,
    var bot: String? = null,
    var botKey: String? = null,
    var model: String? = null,
    var remoteEndpoint: String? = null,
    var ttsConfig: TtsConfig? = null
)