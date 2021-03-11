package ai.flowstorm.common.resources

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import ai.flowstorm.common.AppConfig
import ai.flowstorm.common.JerseyApplication
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
            config.get("name", "unknown"),
            config.get("namespace", "unknown"),
            config.get("package", JerseyApplication.instance::class.java.`package`.name),
            config.get("base.ref", "unknown"),
            config.get("git.ref", "unknown"),
            config.get("git.commit", "unknown"),
            config.get("app.image", "unknown")
        )
    }
}