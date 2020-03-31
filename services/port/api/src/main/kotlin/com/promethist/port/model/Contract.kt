package com.promethist.port.model

import com.promethist.core.model.TtsConfig

data class Contract(
    var name: String? = null,
    var appKey: String? = null,
    var bot: String? = null,
    var botKey: String? = null,
    var model: String? = null,
    var subDialogueModels: List<String>? = null,
    var remoteEndpoint: String? = null,
    var ttsConfig: TtsConfig? = null,
    var sttAudioSave: Boolean = false,
    var language: String = "en"
)