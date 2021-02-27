package ai.flowstorm.core.builder.resources

import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import ai.flowstorm.core.builder.Info
import ai.flowstorm.core.builder.Request
import ai.flowstorm.core.builder.Response
import ai.flowstorm.core.model.EntityDataset
import ai.flowstorm.security.Authenticated
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["Builder"])
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuilderResource {

    @GET
    @Path("/info")
    fun info(): Info

    @POST
    @Path("/build")
    @Authenticated
    fun build(@ApiParam("Request", required = true) request: Request): Response

    @POST
    @Path("/train/entity")
    fun trainEntityModel(dataset: EntityDataset)
}