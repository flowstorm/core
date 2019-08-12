package com.promethistai.port.bot

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Authorization
import java.io.Serializable
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(description = "Bot service")
interface BotService {

    data class Response(
        var answer: String = "",
        var confidence: Double = 1.0) : Serializable

    @GET
    @Path("bot/message")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Message to bot", authorizations = [
        Authorization("key")
    ])
    fun message(@QueryParam("key") key: String, @QueryParam("text") text: String): Response

    @GET
    @Path("bot/welcome")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Welcome message from bot", authorizations = [
        Authorization("key")
    ])
    fun welcome(@QueryParam("key") key: String): String

}
