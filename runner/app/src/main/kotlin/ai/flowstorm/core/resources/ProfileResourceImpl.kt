package ai.flowstorm.core.resources

import org.litote.kmongo.Id
import ai.flowstorm.common.query.Query
import ai.flowstorm.common.security.Authorized
import ai.flowstorm.core.model.Profile
import ai.flowstorm.core.repository.ProfileRepository
import javax.inject.Inject
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/profiles")
@Produces(MediaType.APPLICATION_JSON)
@Authorized
class ProfileResourceImpl : ProfileResource {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var query: Query

    override fun find(): List<Profile> = profileRepository.find(query)

    override fun get(profileId: Id<Profile>): Profile = profileRepository.get(profileId)

    override fun create(profile: Profile) = error("Not supported")

    override fun update(profileId: Id<Profile>, profile: Profile) = error("Not supported")
}