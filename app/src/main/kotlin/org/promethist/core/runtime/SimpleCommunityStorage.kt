package org.promethist.core.runtime

import org.promethist.core.repository.CommunityRepository
import org.promethist.core.model.Community

class SimpleCommunityStorage : CommunityRepository {

    val communities = mutableMapOf<String, Community>()

    override fun getCommunitiesInSpace(spaceId: String): List<Community> {
        return communities.values.filter { it.organization_id == spaceId }
    }

    override fun get(communityName: String, spaceId: String): Community? = communities[communityName]

    override fun create(community: Community) {
        communities[community.name] = community
    }

    override fun update(community: Community) {
        communities[community.name] = community
    }
}