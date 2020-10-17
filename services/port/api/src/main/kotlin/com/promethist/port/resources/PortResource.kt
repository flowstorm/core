package com.promethist.port.resources

import com.promethist.core.resources.CoreResource
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response as JerseyResponse

@Api(description = "Port resource")
interface PortResource : CoreResource {

    @GET
    @Path("proxy/{spec : .+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun proxyFile(
            @ApiParam(required = true) @PathParam("spec") spec: String
    ): JerseyResponse
}