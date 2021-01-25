package org.promethist.core.repository

import org.promethist.common.query.Query
import org.promethist.core.model.DialogueEvent

interface EventRepository : EntityRepository<DialogueEvent> {
    fun getDialogueEvents(query: Query): List<DialogueEvent>
}