package com.promethistai.port.resources

import com.promethistai.port.bot.BotService
import com.promethistai.port.model.Contract
import com.promethistai.port.model.Message
import com.promethistai.port.tts.TtsVoice
import io.swagger.annotations.Api

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Authorization
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(description = "Port resource")
interface PortResource : BotService {

    @GET
    @Path("contract")
    @ApiOperation(value = "Get port contract")
    @Produces(MediaType.APPLICATION_JSON)
    fun getContract(@ApiParam("App key", required = true) @QueryParam("key") appKey: String): Contract

    @PUT
    @Path("message/_queue")
    @ApiOperation(value = "Push message to queue")
    @Produces(MediaType.APPLICATION_JSON)
    fun messageQueuePush(@ApiParam("App key", required = true) @QueryParam("key") appKey: String,
                         @ApiParam("Message", required = true) message: Message): Boolean

    @GET
    @Path("message/_queue")
    @ApiOperation(value = "Pop messages from queue")
    @Produces(MediaType.APPLICATION_JSON)
    fun messageQueuePop(@ApiParam("App key", required = true) @QueryParam("key") appKey: String,
                        @ApiParam("Recipient", required = true) @QueryParam("recipient") recipient: String,
                        @ApiParam(defaultValue = "1") @QueryParam("limit") limit: Int = 1): List<Message>

    @GET
    @Path("file/{id}")
    @ApiOperation("Read resource file", authorizations = [
        Authorization("key")
    ])
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun readFile(
            @ApiParam(required = true) @PathParam("id") id: String
    ): Response

    @POST
    @Path("tts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Perform TTS")
    fun tts(@ApiParam("App key", required = true)
            @QueryParam("key") appKey: String,
            @QueryParam("provider") @DefaultValue("google") provider: String, speechText: String): ByteArray

    @GET
    @Path("tts/voices")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get list of available voices for TTS")
    fun ttsVoices(@QueryParam("provider") @DefaultValue("google") provider: String): List<TtsVoice>


}