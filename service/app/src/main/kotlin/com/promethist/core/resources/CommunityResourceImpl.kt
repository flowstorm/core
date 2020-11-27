package com.promethist.core.resources

import com.mongodb.client.MongoDatabase
import com.promethist.core.model.Community
import org.litote.kmongo.*
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/communities")
@Produces(MediaType.APPLICATION_JSON)
class CommunityResourceImpl : CommunityResource {

    @Inject
    lateinit var database: MongoDatabase

    private val communities by lazy { database.getCollection<Community>() }
    override fun getCommunities(): List<Community> {
        return communities.find().toMutableList()
    }

    override fun getCommunitiesInOrganization(organizationId: String): List<Community> {
        return communities.find(Community::organization_id eq organizationId).toMutableList()
    }

    override fun get(communityName: String, organizationId: String): Community? {
        return communities.find(Community::name eq communityName, Community::organization_id eq organizationId).singleOrNull()
    }

    override fun create(community: Community) {
        communities.insertOne(community)
    }

    override fun update(community: Community) {
        communities.updateOneById(community._id, community, upsert())
    }
}