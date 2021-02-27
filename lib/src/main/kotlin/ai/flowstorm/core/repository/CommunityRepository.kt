package ai.flowstorm.core.repository

import ai.flowstorm.core.model.Community

interface CommunityRepository {

    fun getCommunitiesInSpace(spaceId: String): List<Community>

    fun get(communityName: String, spaceId: String): Community?

    fun create(community: Community)

    fun update(community: Community)
}