package com.promethistai.port.resources

import com.promethistai.port.ConfigService
import com.promethistai.port.PortResource
import com.promethistai.port.bot.BotService
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsServiceFactory
import com.promethistai.port.tts.TtsVoice
import javax.inject.Inject
import javax.ws.rs.*

@Path("/")
class PortResourceImpl : PortResource {

    /**
     * Example of dependency injection
     * @see com.promethistai.port.Application constructor
     */
    @Inject
    lateinit var botService: BotService

    @Inject
    lateinit var configService: ConfigService

    override fun getConfig(key: String): PortConfig = configService.getConfig(key)

    override fun botText(key: String, text: String): BotService.Response {
        return botService.process(key, text)
    }

    override fun tts(provider: String, request: TtsRequest): ByteArray {
        return TtsServiceFactory.create(provider).speak(request.text!!, request.voice!!, request.language!!)
    }

    override fun ttsVoices(provider: String): List<TtsVoice> {
        return TtsServiceFactory.create(provider).voices
    }

    override fun ttsBR(request: TtsRequest): ByteArray {
        return tts("google", request)
    }

    override fun ttsVoicesBR(): List<TtsVoice> {
        return ttsVoices("google")
    }
}