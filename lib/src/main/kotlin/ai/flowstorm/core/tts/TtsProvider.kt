package ai.flowstorm.core.tts

interface TtsProvider {

    val name: String

    fun speak(ttsRequest: TtsRequest): ByteArray
}