package com.promethist.core

import com.promethist.core.model.Community

interface CommunityStorage {

    fun getCommunitiesInOrganization(organizationId: String): List<Community>

    fun get(communityName: String, organizationId: String): Community?

    fun create(community: Community)

    fun update(community: Community)
}