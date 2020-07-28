package com.promethist.core.resources

import com.mongodb.client.MongoDatabase
import com.promethist.core.model.Community
import org.litote.kmongo.*
import javax.inject.Inject

class CommunityResourceImpl : CommunityResource {

    @Inject
    lateinit var database: MongoDatabase

    private val communities by lazy { database.getCollection<Community>() }
    override fun getCommunities(): List<Community> {
        return communities.find().toMutableList()
    }

    override fun get(communityName: String): Community? {
        return communities.find(Community::name eq communityName).singleOrNull()
    }

    override fun create(community: Community) {
        communities.insertOne(community)
    }

    override fun update(community: Community) {
        communities.updateOneById(community._id, community, upsert())
    }
}