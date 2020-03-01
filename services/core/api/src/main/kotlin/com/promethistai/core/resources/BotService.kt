package com.promethistai.core.resources

import com.promethistai.core.model.Message
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Authorization
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
    fun message(@ApiParam("App key", required = true) @QueryParam("key") appKey: String,
                @ApiParam("Message", required = true) message: Message): Message?


}