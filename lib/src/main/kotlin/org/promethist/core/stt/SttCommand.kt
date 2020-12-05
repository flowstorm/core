package org.promethist.core.stt

import org.promethist.core.type.PropertyMap
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