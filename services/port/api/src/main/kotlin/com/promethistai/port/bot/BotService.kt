package com.promethistai.port.bot

import io.swagger.annotations.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(description = "Bot service")
interface BotService {

    /**
     * Message should always contain following properties
     * - text = message text
     * - key = client key
     * @return Message if there is direct response, otherwise null
     */
    @PUT
    @Path("message")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Message to bot", authorizations = [
        Authorization("key")
    ])
    //@ApiResponse
    fun message(@ApiParam("Client key", required = true) @QueryParam("key") key: String,
                @ApiParam("Message", required = true) message: Message): Message?


}
