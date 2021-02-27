package ai.flowstorm.core.tts

interface TtsService {

    fun speak(ttsRequest: TtsRequest): ByteArray
}