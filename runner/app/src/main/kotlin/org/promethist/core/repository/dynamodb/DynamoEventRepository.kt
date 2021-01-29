@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package org.promethist.core.repository.dynamodb

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.KeyAttribute
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.litote.kmongo.Id
import org.promethist.common.ObjectUtil
import org.promethist.common.query.DynamoDbFiltersFactory
import org.promethist.common.query.Query
import org.promethist.core.model.DialogueEvent
import org.promethist.core.model.Session
import org.promethist.core.repository.EventRepository
import java.util.*

class DynamoEventRepository : DynamoAbstractEntityRepository<DialogueEvent>(), EventRepository {

    private val dialogueEventTable by lazy { database.getTable(tableName("dialogueEvent")) }

    override fun getDialogueEvents(query: Query): List<DialogueEvent> {
        val spec = QuerySpec()
        var datetime: Date? = null
        if (query.seek_id != null) {
            datetime = ObjectUtil.defaultMapper.readValue(dialogueEventTable.getItem(KeyAttribute("_id", query.seek_id)).toJSON(), Session::class.java).datetime
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

        return dialogueEventTable.getIndex("space_id").query(spec).map { item -> ObjectUtil.defaultMapper.readValue(item.toJSON(), DialogueEvent::class.java)}
    }

    override fun getAll(): List<DialogueEvent> {
        return dialogueEventTable.scan().toList().map { item -> ObjectUtil.defaultMapper.readValue(item.toJSON(), DialogueEvent::class.java) }
    }

    override fun get(id: Id<DialogueEvent>): DialogueEvent? {
        return dialogueEventTable.getItem(KeyAttribute("_id", id.toString()))?.let {
            ObjectUtil.defaultMapper.readValue(it.toJSON(), DialogueEvent::class.java)
        }
    }

    override fun find(query: Query): List<DialogueEvent> = getDialogueEvents(query)

    override fun create(dialogueEvent: DialogueEvent): DialogueEvent {
        dialogueEventTable.putItem(Item.fromJSON(ObjectUtil.defaultMapper.writeValueAsString(dialogueEvent)))
        return dialogueEvent
    }

    override fun update(entity: DialogueEvent, upsert: Boolean): DialogueEvent {
        return create(entity)
    }

}