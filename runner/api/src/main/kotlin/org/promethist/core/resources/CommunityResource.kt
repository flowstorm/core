package org.promethist.core.resources

import org.promethist.core.repository.CommunityRepository
import org.promethist.core.model.Community
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["Communities"])
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface CommunityResource : CommunityRepository {
    @GET
    fun getCommunities(): List<Community>

    @GET
    @Path("/{spaceId}")
    override fun getCommunitiesInSpace(
            @ApiParam(required = true) @PathParam("spaceId") spaceId: String
    ): List<Community>

    @GET
    @Path("/{spaceId}/community/{communityName}")
    override fun get(
            @ApiParam(required = true) @PathParam("communityName") communityName: String,
            @ApiParam(required = true) @PathParam("spaceId") spaceId: String
    ): Community?

    @POST
    override fun create(
            @ApiParam(required = true) community: Community
    )

    @PUT
    @Path("/{communityId}")
    override fun update(
            @ApiParam(required = true) community: Community
    )
}