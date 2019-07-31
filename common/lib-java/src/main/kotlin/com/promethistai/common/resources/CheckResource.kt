package com.promethistai.common.resources

import com.promethistai.common.Config
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("check")
@Produces(MediaType.APPLICATION_JSON)
@Api(description = "Check of service health and status")
class CheckResource {

    @GET
    @ApiOperation("Check service health and basic config parameters")
    fun getCheck(): Check {
        val config = Config.instance
        return Check(
                1.0,
                config["name"],
                config["namespace"],
                config["git.ref"],
                config["git.commit"],
                config["app.image"])
    }
}