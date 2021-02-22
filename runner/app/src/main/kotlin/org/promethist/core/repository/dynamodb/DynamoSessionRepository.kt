@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package org.promethist.core.repository.dynamodb

import com.amazonaws.services.dynamodbv2.document.KeyAttribute
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec
import com.amazonaws.services.dynamodbv2.document.utils.NameMap
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.litote.kmongo.Id
import org.promethist.common.ObjectUtil
import org.promethist.common.query.DynamoDbFiltersFactory
import org.promethist.common.query.Query
import org.promethist.common.repository.DynamoAbstractEntityRepository
import org.promethist.core.model.Session
import org.promethist.core.model.User
import org.promethist.core.repository.SessionRepository
import java.util.*


class DynamoSessionRepository : DynamoAbstractEntityRepository<Session>(), SessionRepository {

    override val table by lazy { database.getTable(tableName("session")) }

    override fun findBy(userId: Id<User>): List<Session> {
        val spec = ScanSpec()
            .withFilterExpression(".#user.#id = :value")
            .withNameMap(NameMap().with("#user", "user").with("#id", "_id"))
            .withValueMap(ValueMap().withString(":value", userId.toString()))
        return table.scan(spec).toEntityList()
    }

    override fun findBy(sessionId: String): Session? {
        val index = table.getIndex("sessionId")
        return index.query(KeyAttribute("sessionId", sessionId)).singleOrNull()?.toEntity()
    }

    override fun find(id: Id<Session>): Session? {
        return table.getItem(KeyAttribute("_id", id.toString()))?.toEntity()
    }

    override fun find(query: Query): List<Session> {
        val spec = QuerySpec()
        var datetime: Date? = null
        if (query.seek_id != null) {
            datetime = ObjectUtil.defaultMapper.readValue(table.getItem(KeyAttribute("_id", query.seek_id)).toJSON(), Session::class.java).datetime
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

        return table.getIndex("space_id").query(spec).toEntityList()
    }

    override fun all(): List<Session> = table.scan().toEntityList()
}

