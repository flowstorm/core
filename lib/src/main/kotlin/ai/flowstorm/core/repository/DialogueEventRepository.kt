package ai.flowstorm.core.repository

import ai.flowstorm.core.model.DialogueEvent

interface DialogueEventRepository {
    fun create(dialogueEvent: DialogueEvent)
}