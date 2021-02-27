package ai.flowstorm.core.runtime

import ai.flowstorm.core.model.Community
import ai.flowstorm.core.repository.CommunityRepository

class SimpleCommunityStorage : CommunityRepository {

    val communities = mutableMapOf<String, Community>()

    override fun getCommunitiesInSpace(spaceId: String): List<Community> {
        return communities.values.filter { it.space_id == spaceId }
    }

    override fun get(communityName: String, spaceId: String): Community? = communities[communityName]

    override fun create(community: Community) {
        communities[community.name] = community
    }

    override fun update(community: Community) {
        communities[community.name] = community
    }
}