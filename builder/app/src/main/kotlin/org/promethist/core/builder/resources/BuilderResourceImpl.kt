package org.promethist.core.builder.resources

import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/builder")
@Produces(MediaType.APPLICATION_JSON)
class BuilderResourceImpl : BuilderResource {

    override fun build() = BuilderResource.BuildResponse(0)
}