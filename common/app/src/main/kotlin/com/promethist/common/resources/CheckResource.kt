package com.promethist.common.resources

import com.promethist.common.AppConfig
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
        val config = AppConfig.instance
        return Check(
                1.0,
                config["name"],
                config["namespace"],
                config["package"],
                config["git.ref"],
                config["git.commit"],
                config["app.image"])
    }
}