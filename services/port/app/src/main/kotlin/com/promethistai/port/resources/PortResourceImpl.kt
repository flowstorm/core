package com.promethistai.port.resources

import com.promethistai.common.AppConfig
import com.promethistai.datastore.resources.ObjectResource
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
    @Inject lateinit var botService: BotService
    @Inject lateinit var objectResource: ObjectResource
    @Inject lateinit var appConfig: AppConfig

    override fun config(id: Long): PortConfig {

        val contract = objectResource.getObject("port", "contract", id, appConfig["apiKey"])!!

        return PortConfig(id, contract)
    }

    override fun bot(text: String): BotService.Response {
        return /*BotServiceFactory.create()*/botService.process(text)
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