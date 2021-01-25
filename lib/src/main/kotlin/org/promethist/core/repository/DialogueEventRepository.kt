package org.promethist.core.repository

import org.promethist.core.model.DialogueEvent

interface DialogueEventRepository {
    fun create(dialogueEvent: DialogueEvent)
}