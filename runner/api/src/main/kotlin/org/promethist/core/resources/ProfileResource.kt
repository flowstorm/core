package org.promethist.core.resources

import org.promethist.core.model.Profile
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import org.litote.kmongo.Id
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["Profiles"])

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ProfileResource {
    @GET
    fun find(): List<Profile>

    @GET
    @Path("/{profileId}")
    fun get(
            @ApiParam(required = true) @PathParam("profileId") profileId: Id<Profile>
    ): Profile

    @POST
    fun create(
            @ApiParam(required = true) profile: Profile
    )

    @PUT
    @Path("/{profileId}")
    fun update(
            @ApiParam(required = true) @PathParam("profileId") profileId: Id<Profile>,
            @ApiParam(required = true) profile: Profile
    )
}