package com.promethistai.skeleton.resources

import io.swagger.annotations.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Api(description = "Sample resource")
class HelloResource {

    @GET
    @Path("hello")
    @ApiOperation(value = "Sample operation", authorizations = [
            Authorization("apiKey")
        ]/*, extensions = [
        Extension(name = "google-quota", properties = [
            ExtensionProperty(name = "name1", value = "value1"),
            ExtensionProperty(name = "name2", value = "value2")
        ])
    ]*/)
    fun getHello(@ApiParam(hidden = true) @QueryParam("key") apiKey: String): Hello {
        return Hello("Hello Kotlin from Skeleton app (apiKey=$apiKey)")
    }

    @GET
    @Path("exception")
    @ApiOperation("Sample exception")
    fun getException() {
        if (true)
            throw WebApplicationException("Sample exception")
    }
}