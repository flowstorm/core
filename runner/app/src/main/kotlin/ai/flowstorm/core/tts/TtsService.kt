package ai.flowstorm.core.tts

import org.glassfish.hk2.api.IterableProvider
import javax.inject.Inject

class TtsService {

    @Inject
    lateinit var boundServices: IterableProvider<TtsProvider>

    fun get(provider: String) = boundServices.find { it.name == provider } ?: error("Unknown TTS service provider: $provider")

    fun speak(request: TtsRequest): ByteArray {
        if (request.isSsml && !request.text.startsWith("<speak>"))
            request.text = "<speak>${request.text}</speak>"
        return get(request.config.provider).speak(request)
    }
}