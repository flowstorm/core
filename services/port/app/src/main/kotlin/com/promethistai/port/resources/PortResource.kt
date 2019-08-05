package com.promethistai.port.resources

import com.promethistai.port.bot.BotService
import com.promethistai.port.tts.TtsService
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsVoice
import io.swagger.annotations.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Produces(MediaType.APPLICATION_JSON)
@Api(description = "Port resource")
@Path("/")
class PortResource {

    @GET
    @Path("config")
    @ApiOperation(value = "Get port configuration")
    fun config(@QueryParam("id") id: String): PortConfig {
        return PortConfig("{\"id\":$id}")
    }

    @GET
    @Path("bot/text")
    @Produces(MediaType.APPLICATION_JSON)
    fun bot(@QueryParam("text") text: String): BotService.Response {
        return BotService.create().process(text)
    }

    @POST
    @Path("tts")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "TTS - converts text from request into speech")
    fun tts(@QueryParam("provider") @DefaultValue("google") provider: String, request: TtsRequest): ByteArray {
        return TtsService.create(provider).speak(request.text!!, request.voice!!, request.language!!)
    }

    @GET
    @Path("tts/voices")
    @Produces(MediaType.APPLICATION_JSON)
    fun ttsVoices(@QueryParam("provider") @DefaultValue("google") provider: String): List<TtsVoice> {
        return TtsService.create(provider).voices
    }

    // bronzerabbit compatibility (TO BE REMOVED)
    @POST
    @Path("/audio/output/")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun ttsBR(request: TtsRequest): ByteArray {
        return tts("google", request)
    }

    @GET
    @Path("/voices/")
    @Produces(MediaType.APPLICATION_JSON)
    fun ttsVoicesBR(): List<TtsVoice> {
        return ttsVoices("google")
    }
}