@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package org.promethist.core.repository.dynamodb

import com.amazonaws.services.dynamodbv2.document.KeyAttribute
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.litote.kmongo.Id
import org.promethist.common.ObjectUtil
import org.promethist.common.query.DynamoDbFiltersFactory
import org.promethist.common.query.Query
import org.promethist.common.repository.DynamoAbstractEntityRepository
import org.promethist.core.model.Session
import org.promethist.core.model.Turn
import org.promethist.core.repository.TurnRepository
import java.util.*

class DynamoTurnRepository: DynamoAbstractEntityRepository<Turn>(), TurnRepository {

    override val table by lazy { database.getTable(tableName("turn")) }

    override fun findBy(session_id: Id<Session>): List<Turn> {
        val spec: QuerySpec = QuerySpec().withKeyConditionExpression("sessionId = :v_id")
            .withValueMap(ValueMap().withString(":v_id", session_id.toString()))
        return table.query(spec).toEntityList()
    }

    override fun findBy(session_ids: List<Id<Session>>): List<Turn> {
        return session_ids.map { findBy(it) }.flatten()
    }

    override fun find(id: Id<Turn>): Turn? {
        return table.getItem(KeyAttribute("_id", id.toString()))?.toEntity()
    }

    override fun find(query: Query): List<Turn> {
        val spec = QuerySpec()
        var datetime: Date? = null
        if (query.seek_id != null) {
            datetime = ObjectUtil.defaultMapper.readValue(table.getItem(KeyAttribute("_id", query.seek_id)).toJSON(), Turn::class.java).datetime
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

    override fun all(): List<Turn> = table.scan().toEntityList()
}