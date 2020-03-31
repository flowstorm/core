package com.promethist.port.resources

import com.promethist.core.model.Message
import com.promethist.core.resources.BotService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Authorization
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(description = "Port resource")
interface PortResource : BotService {

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
}