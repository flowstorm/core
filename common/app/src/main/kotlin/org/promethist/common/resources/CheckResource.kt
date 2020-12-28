package org.promethist.common.resources

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.promethist.common.AppConfig
import org.promethist.common.JerseyApplication
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
                config.get("package", JerseyApplication.instance::class.java.`package`.name),
                config.get("base.ref", "unknown"),
                config["git.ref"],
                config["git.commit"],
                config["app.image"]
        )
    }
}