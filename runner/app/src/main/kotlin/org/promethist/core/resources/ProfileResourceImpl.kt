package org.promethist.core.resources

import org.promethist.common.query.Query
import org.promethist.core.model.Profile
import org.litote.kmongo.*
import org.promethist.core.repository.ProfileRepository
import javax.inject.Inject
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/profiles")
@Produces(MediaType.APPLICATION_JSON)
class ProfileResourceImpl : ProfileResource {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var query: Query


    override fun find(): List<Profile> = profileRepository.find(query)

    override fun get(profileId: Id<Profile>): Profile {
        return profileRepository.get(profileId)
            ?: throw NotFoundException("Profile $profileId not found.")
    }

    override fun create(profile: Profile) = error("Not supported")

    override fun update(profileId: Id<Profile>, profile: Profile) = error("Not supported")
}