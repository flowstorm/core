package org.promethist.client.audio

class WakeWordConfig(val type: Type, val dir: String = ".", val libFile: String? = null, val sensitivity: Float = 0.6F, val gain: Float = 1.0F) {
    enum class Type { snowboy, none }
}