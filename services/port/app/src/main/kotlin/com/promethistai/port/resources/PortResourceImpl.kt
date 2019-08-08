package com.promethistai.port.resources

import com.promethistai.common.AppConfig
import com.promethistai.datastore.resources.Object
import com.promethistai.datastore.resources.ObjectResource
import com.promethistai.port.PortResource
import com.promethistai.port.bot.BotService
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsServiceFactory
import com.promethistai.port.tts.TtsVoice
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.Response

@Path("/")
class PortResourceImpl : PortResource {

    /**
     * Example of dependency injection
     * @see com.promethistai.port.Application constructor
     */
    @Inject lateinit var botService: BotService
    @Inject lateinit var objectResource: ObjectResource
    @Inject lateinit var appConfig: AppConfig

    override fun getConfig(key: String): PortConfig {
        /*
        val contracts = objectResource.queryObjects("port", appConfig["apiKey"],
                "SELECT * FROM contract WHERE key=@key",
                Object().set("key", key))
        */
        val contracts = objectResource.filterObjects("port", "contract", appConfig["apiKey"], Object().set("key", key))

        return if (contracts.isEmpty())
            throw WebApplicationException(Response.Status.NOT_FOUND)
        else
            PortConfig(appConfig["service.host"], contracts[0])
    }

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