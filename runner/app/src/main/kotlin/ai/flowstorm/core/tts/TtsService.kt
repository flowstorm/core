package ai.flowstorm.core.tts

import ai.flowstorm.core.AudioFileType
import ai.flowstorm.core.util.AudioUtil
import org.glassfish.hk2.api.IterableProvider
import javax.inject.Inject

class TtsService {

    @Inject
    lateinit var boundServices: IterableProvider<TtsProvider>

    fun get(provider: String) = boundServices.find { it.name == provider } ?: error("Unknown TTS service provider: $provider")

    fun speak(request: TtsRequest, fileType: AudioFileType): ByteArray {
        if (request.isSsml && !request.text.startsWith("<speak>"))
            request.text = "<speak>${request.text}</speak>"
        val data = get(request.config.provider).speak(request)
        return if (fileType == AudioFileType.mp3) data else AudioUtil.convert(data, fileType, request.hash())
    }
}