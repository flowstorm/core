package com.promethistai.port

data class AppClientManifest(
    var sttAudioInputStream: String? = null,
    var ttsAudioOutput: String? = null,
    var ttsVoice: String? = null,
    var channel: String? = null
)