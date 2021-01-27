@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package org.promethist.core.repository.dynamodb

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.KeyAttribute
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec
import com.amazonaws.services.dynamodbv2.document.utils.NameMap
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import org.litote.kmongo.Id
import org.promethist.common.ObjectUtil
import org.promethist.common.query.DynamoDbFiltersFactory
import org.promethist.common.query.Query
import org.promethist.core.model.Session
import org.promethist.core.model.User
import org.promethist.core.repository.SessionRepository


class DynamoSessionRepository : DynamoAbstractEntityRepository<Session>(), SessionRepository {

    private val sessionsTable by lazy { database.getTable(tableName("session")) }

    override fun getSessions(query: Query): List<Session> {
        val spec = ScanSpec()
        val (filterExpression, nameMap, valueMap) = DynamoDbFiltersFactory.createFilters(query)

        if (query.seek_id != null) {
            val datetime = ObjectUtil.defaultMapper.readValue(sessionsTable.getItem(KeyAttribute("_id", query.seek_id)).toJSON(), Session::class.java).datetime
            DynamoDbFiltersFactory.handleDatetime(query, datetime, filterExpression, nameMap, valueMap)
        }

        spec.withFilterExpression(filterExpression.joinToString(separator = " and "))
        spec.withNameMap(nameMap)
        spec.withValueMap(valueMap)
        spec.withMaxResultSize(query.limit)
        return sessionsTable.scan(spec).map { item -> ObjectUtil.defaultMapper.readValue(item.toJSON(), Session::class.java) }.sortedByDescending { item -> item.datetime }
    }

    override fun getForUser(userId: Id<User>): List<Session> {
        val spec = ScanSpec()
            .withFilterExpression(".#user.#id = :value")
            .withNameMap(NameMap().with("#user", "user").with("#id", "_id"))
            .withValueMap(ValueMap().withString(":value", userId.toString()))
        return sessionsTable.scan(spec).map { item -> ObjectUtil.defaultMapper.readValue(item.toJSON(), Session::class.java) }
    }

    override fun get(sessionId: String): Session? {
        val index = sessionsTable.getIndex("sessionId")
        return index.query(KeyAttribute("sessionId", sessionId)).singleOrNull()?.let {
            ObjectUtil.defaultMapper.readValue(it.toJSON(), Session::class.java)
        }
    }


    override fun get(id: Id<Session>): Session? {
        return sessionsTable.getItem(KeyAttribute("_id", id.toString()))?.let {
            ObjectUtil.defaultMapper.readValue(it.toJSON(), Session::class.java)
        }
    }

    override fun find(query: Query): List<Session> {
        val spec = ScanSpec()
        val (filterExpression, nameMap, valueMap) = DynamoDbFiltersFactory.createFilters(query)

        spec.withFilterExpression(filterExpression.joinToString(separator = " and "))
        spec.withNameMap(nameMap)
        spec.withValueMap(valueMap)
        spec.withMaxResultSize(query.limit)
        return sessionsTable.scan(spec).map { item -> ObjectUtil.defaultMapper.readValue(item.toJSON(), Session::class.java) }
    }

    override fun getAll(): List<Session> = sessionsTable.scan().toList().map { item -> ObjectUtil.defaultMapper.readValue(item.toJSON(), Session::class.java) }

    override fun create(session: Session): Session {
        sessionsTable.putItem(Item.fromJSON(ObjectUtil.defaultMapper.writeValueAsString(session)))
        return session
    }

    override fun update(entity: Session, upsert: Boolean): Session {
        return create(entity) // it will be updated if the id is the same
    }

}

