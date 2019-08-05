package com.promethistai.port.stt

import java.io.Serializable

data class SttCommand(
    var type: Type? = null,
    var params: Map<String, Any>? = null) : Serializable {

    enum class Type {
        Init,
        Pause,
        Resume
    }

}