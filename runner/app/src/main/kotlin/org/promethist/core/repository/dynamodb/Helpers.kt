package org.promethist.core.repository.dynamodb

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.KeyAttribute
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import org.promethist.common.ObjectUtil
import org.promethist.core.model.Session
import org.promethist.core.model.Turn

class Helpers {
    companion object {

    fun Item.toSession(turnsTable: Table, limit: Int = 0): Session {
        val session = ObjectUtil.defaultMapper.readValue(this.toJSON(), Session::class.java)
        val spec: QuerySpec = QuerySpec().withKeyConditionExpression("sessionId = :v_id")
            .withValueMap(ValueMap().withString(":v_id", session._id.toString()))
        if (limit > 0) spec.withMaxPageSize(limit)
        session.turns.addAll(turnsTable
            .query(spec)
            .map { item -> ObjectUtil.defaultMapper.readValue(item.toJSON(), Turn::class.java) })
        return session
    }

    fun Session.toItem(turnsTable: Table): Item {
        // deepcopy - otherwise problem with clearing the session
        val copyToSave = ObjectUtil.defaultMapper.readValue(Item.fromJSON(ObjectUtil.defaultMapper.writeValueAsString(this)).toJSON(), this::class.java)
        for (turn in copyToSave.turns) {
            turn.sessionId = copyToSave._id
            turnsTable.putItem(Item.fromJSON(ObjectUtil.defaultMapper.writeValueAsString(turn)))
        }
        copyToSave.turns.clear()
        return Item.fromJSON(ObjectUtil.defaultMapper.writeValueAsString(copyToSave))
    }
}
}