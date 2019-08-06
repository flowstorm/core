package com.promethistai.port.resources

import com.promethistai.port.PortResource
import com.promethistai.port.bot.BotService
import com.promethistai.port.bot.BotServiceFactory
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsServiceFactory
import com.promethistai.port.tts.TtsVoice
import javax.ws.rs.*

@Path("/")
class PortResourceImpl : PortResource {

    override fun config(id: String): PortConfig {
        return PortConfig(id)
    }

    override fun bot(text: String): BotService.Response {
        return BotServiceFactory.create().process(text)
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