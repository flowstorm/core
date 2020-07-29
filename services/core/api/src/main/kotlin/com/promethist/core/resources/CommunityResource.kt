package com.promethist.core.resources

import com.promethist.core.model.Community
import com.promethist.core.model.Profile
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import org.litote.kmongo.Id
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["Communities"])
@Path("/communities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface CommunityResource {
    @GET
    fun getCommunities(): List<Community>

    @GET
    @Path("/{organizationId}")
    fun getCommunitiesInOrganization(
            @ApiParam(required = true) @PathParam("organizationId") organizationId: String
    ): List<Community>

    @GET
    @Path("/community/{communityName}")
    fun get(
            @ApiParam(required = true) @PathParam("communityName") communityName: String
    ): Community?

    @POST
    fun create(
            @ApiParam(required = true) community: Community
    )

    @PUT
    @Path("/{communityId}")
    fun update(
            @ApiParam(required = true) community: Community
    )
}