package com.promethistai.common.resources

import io.swagger.annotations.Api
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(hidden = true)
@Path("/")
class SwaggerResource {

    @GET
    @Path("/swagger.json")
    fun getJsonFile(): Response {
        return Response.ok(this::class.java.getResourceAsStream("/swagger.json"), MediaType.APPLICATION_JSON).build()
    }

    @GET
    @Path("/swagger.yaml")
    fun getYamlFile(): Response {
        return Response.ok(this::class.java.getResourceAsStream("/swagger.yaml"), "application/yaml").build()
    }
}