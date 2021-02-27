package ai.flowstorm.core.stt

import ai.flowstorm.core.type.PropertyMap
import java.io.Serializable

data class SttCommand(
        var type: Type? = null,
        var params: PropertyMap? = null) : Serializable {

    enum class Type {
        Init,
        Pause,
        Resume
    }

}