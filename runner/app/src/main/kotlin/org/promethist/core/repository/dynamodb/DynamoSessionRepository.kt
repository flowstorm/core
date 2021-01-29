@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package org.promethist.core.repository.dynamodb

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.KeyAttribute
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec
import com.amazonaws.services.dynamodbv2.document.utils.NameMap
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.litote.kmongo.Id
import org.promethist.common.ObjectUtil
import org.promethist.common.query.DynamoDbFiltersFactory
import org.promethist.common.query.Query
import org.promethist.core.model.Session
import org.promethist.core.model.Turn
import org.promethist.core.model.User
import org.promethist.core.repository.SessionRepository
import java.util.*


class DynamoSessionRepository : DynamoAbstractEntityRepository<Session>(), SessionRepository {

    private val sessionsTable by lazy { database.getTable(tableName("session")) }
    private val turnsTable by lazy { database.getTable(tableName("turn")) }

    override fun getSessions(query: Query): List<Session> {
        val spec = QuerySpec()
        var datetime: Date? = null
        if (query.seek_id != null) {
            datetime = ObjectUtil.defaultMapper.readValue(sessionsTable.getItem(KeyAttribute("_id", query.seek_id)).toJSON(), Session::class.java).datetime
        }
        val (filterExpression, keywordExpression, nameMap, valueMap) = DynamoDbFiltersFactory.createFilters(query, indexValues=mutableListOf("space_id", "datetime"), datetime=datetime)

        if (query.seek_id != null) {
            filterExpression.add("( #id <> :id )")
            nameMap.with("#id", "_id")
            valueMap.withString(":id", query.seek_id)
        }
        filterExpression.ifNotEmpty { spec.withFilterExpression(this.joinToString(separator = " and ")) }
        keywordExpression.ifNotEmpty { spec.withKeyConditionExpression(this.joinToString(separator = " and ")) }
        spec.withNameMap(nameMap)
        spec.withValueMap(valueMap)
        spec.withMaxResultSize(query.limit)
        spec.withScanIndexForward(false)

        return sessionsTable.getIndex("space_id").query(spec).map { item -> item.toSession(turnsTable) }
    }

    override fun getForUser(userId: Id<User>): List<Session> {
        val spec = ScanSpec()
            .withFilterExpression(".#user.#id = :value")
            .withNameMap(NameMap().with("#user", "user").with("#id", "_id"))
            .withValueMap(ValueMap().withString(":value", userId.toString()))
        return sessionsTable.scan(spec).map { item -> item.toSession(turnsTable) }
    }

    override fun get(sessionId: String): Session? {
        val index = sessionsTable.getIndex("sessionId")
        return index.query(KeyAttribute("sessionId", sessionId)).singleOrNull()?.toSession(turnsTable, limit = 10)
    }


    override fun get(id: Id<Session>): Session? {
        return sessionsTable.getItem(KeyAttribute("_id", id.toString()))?.toSession(turnsTable)
    }

    override fun find(query: Query): List<Session> = getSessions(query)

    override fun getAll(): List<Session> = sessionsTable.scan().toList().map { item -> item.toSession(turnsTable) }

    override fun create(session: Session): Session {
        sessionsTable.putItem(session.toItem(turnsTable))
        return session
    }

    override fun update(entity: Session, upsert: Boolean): Session {
        return create(entity) // it will be updated if the id is the same
    }


    companion object {
        fun Item.toSession(turnsTable: Table, limit: Int = 0): Session {
            val session = ObjectUtil.defaultMapper.readValue(this.toJSON(), Session::class.java)
            val spec: QuerySpec = QuerySpec().withKeyConditionExpression("sessionId = :v_id")
                .withValueMap(ValueMap().withString(":v_id", session._id.toString()))
            if (limit > 0) {
                spec.withMaxPageSize(limit)
            }
            session.turns.addAll(turnsTable
                .query(spec)
                .map { item -> ObjectUtil.defaultMapper.readValue(item.toJSON(), Turn::class.java) })
            return session
        }

        fun Session.toItem(turnsTable: Table): Item {
            turns.forEach { turn ->
                turn.sessionId = _id
                turnsTable.putItem(Item.fromJSON(ObjectUtil.defaultMapper.writeValueAsString(turn)))
            }
            val turnsBackup = turns
            turns = mutableListOf()
            return Item.fromJSON(ObjectUtil.defaultMapper.writeValueAsString(this)).also {
                turns = turnsBackup
            }
        }
    }
}

