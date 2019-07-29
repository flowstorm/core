package com.promethistai.skeleton.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/java")
@Produces(MediaType.APPLICATION_JSON)
@Api(description = "Sample resource in Java")
public class HelloJavaResource {

    @GET
    @ApiOperation("Sample operation in Java")
    public Hello getHello() {
        return new Hello("Hello Java from Skeleton app!");
    }
}