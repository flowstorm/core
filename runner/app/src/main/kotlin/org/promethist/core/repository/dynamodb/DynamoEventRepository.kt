@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package org.promethist.core.repository.dynamodb

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.KeyAttribute
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec
import org.litote.kmongo.Id
import org.promethist.common.ObjectUtil
import org.promethist.common.query.DynamoDbFiltersFactory
import org.promethist.common.query.Query
import org.promethist.core.model.DialogueEvent
import org.promethist.core.repository.EventRepository

class DynamoEventRepository : DynamoAbstractEntityRepository<DialogueEvent>(), EventRepository {

    private val dialogueEventTable by lazy { database.getTable("dialogueEvent") }

    override fun getDialogueEvents(query: Query): List<DialogueEvent> {
        val spec = ScanSpec()
        val (filterExpression, nameMap, valueMap) = DynamoDbFiltersFactory.createFilters(query)

        if (query.seek_id != null) {
            val datetime = ObjectUtil.defaultMapper.readValue(dialogueEventTable.getItem(KeyAttribute("_id", query.seek_id)).toJSON(), DialogueEvent::class.java).datetime
            DynamoDbFiltersFactory.handleDatetime(query, datetime, filterExpression, nameMap, valueMap)
        }

        spec.withFilterExpression(filterExpression.joinToString(separator = " and "))
        spec.withNameMap(nameMap)
        spec.withValueMap(valueMap)
        spec.withMaxResultSize(query.limit)
        return dialogueEventTable.scan(spec).map { item -> ObjectUtil.defaultMapper.readValue(item.toJSON(), DialogueEvent::class.java) }.sortedByDescending { item -> item.datetime }
    }

    override fun getAll(): List<DialogueEvent> {
        return dialogueEventTable.scan().toList().map { item -> ObjectUtil.defaultMapper.readValue(item.toJSON(), DialogueEvent::class.java) }
    }

    override fun get(id: Id<DialogueEvent>): DialogueEvent? {
        return dialogueEventTable.getItem(KeyAttribute("_id", id.toString()))?.let {
            ObjectUtil.defaultMapper.readValue(it.toJSON(), DialogueEvent::class.java)
        }
    }

    override fun find(query: Query): List<DialogueEvent> {
        val spec = ScanSpec()
        val (filterExpression, nameMap, valueMap) = DynamoDbFiltersFactory.createFilters(query)

        spec.withFilterExpression(filterExpression.joinToString(separator = " and "))
        spec.withNameMap(nameMap)
        spec.withValueMap(valueMap)

        spec.withMaxResultSize(query.limit)
        return dialogueEventTable.scan(spec).map {item -> ObjectUtil.defaultMapper.readValue(item.toJSON(), DialogueEvent::class.java) }
    }

    override fun create(dialogueEvent: DialogueEvent): DialogueEvent {
        dialogueEventTable.putItem(Item.fromJSON(ObjectUtil.defaultMapper.writeValueAsString(dialogueEvent)))
        return dialogueEvent
    }

    override fun update(entity: DialogueEvent, upsert: Boolean): DialogueEvent {
        return create(entity)
    }

}