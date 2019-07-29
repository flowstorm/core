package com.promethistai.skeleton.resources

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Api(description = "Sample resource")
class HelloResource {

    @GET
    @ApiOperation("Sample operation")
    fun getHello(): Hello {
        return Hello("Hello Kotlin from Skeleton app!")
    }

    @GET
    @Path("exception")
    @ApiOperation("Sample exception")
    fun getException() {
        if (true)
            throw WebApplicationException("Sample exception")
    }
}