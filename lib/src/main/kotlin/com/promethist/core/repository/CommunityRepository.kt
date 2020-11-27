package com.promethist.core.repository

import com.promethist.core.model.Community

interface CommunityRepository {

    fun getCommunitiesInOrganization(organizationId: String): List<Community>

    fun get(communityName: String, organizationId: String): Community?

    fun create(community: Community)

    fun update(community: Community)
}