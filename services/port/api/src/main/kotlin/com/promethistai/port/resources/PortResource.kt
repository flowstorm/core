package com.promethistai.port

import com.promethistai.port.bot.BotService
import com.promethistai.port.resources.PortConfig
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsVoice
import io.swagger.annotations.Api

import io.swagger.annotations.ApiOperation
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(description = "Port resource")
interface PortResource {

    @GET
    @Path("config")
    @ApiOperation(value = "Get port configuration")
    @Produces(MediaType.APPLICATION_JSON)
    fun getConfig(@QueryParam("key") key: String): PortConfig

    @GET
    @Path("bot/text")
    @Produces(MediaType.APPLICATION_JSON)
    fun botText(@QueryParam("key") key: String, @QueryParam("text") text: String): BotService.Response

    @POST
    @Path("tts")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Perform TTS")
    fun tts(@QueryParam("provider") @DefaultValue("google") provider: String, request: TtsRequest): ByteArray

    @GET
    @Path("tts/voices")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get list of available voices for TTS")
    fun ttsVoices(@QueryParam("provider") @DefaultValue("google") provider: String): List<TtsVoice>

    // bronzerabbit compatibility (TO BE REMOVED)
    @POST
    @Path("audio/output")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(hidden = true, value = "BronzeRabbit backward compatibility (see tts)")
    fun ttsBR(request: TtsRequest): ByteArray

    @GET
    @Path("voices")
    @ApiOperation(hidden = true, value = "BronzeRabbit backward compatibility (see tts/voices)")
    @Produces(MediaType.APPLICATION_JSON)
    fun ttsVoicesBR(): List<TtsVoice>

}