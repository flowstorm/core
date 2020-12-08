package org.promethist.core.repository

import org.promethist.core.model.Community

interface CommunityRepository {

    fun getCommunitiesInSpace(spaceId: String): List<Community>

    fun get(communityName: String, spaceId: String): Community?

    fun create(community: Community)

    fun update(community: Community)
}