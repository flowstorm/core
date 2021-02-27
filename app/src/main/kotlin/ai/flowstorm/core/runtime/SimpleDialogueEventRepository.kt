package ai.flowstorm.core.runtime

import ai.flowstorm.core.model.DialogueEvent
import ai.flowstorm.core.repository.DialogueEventRepository

class SimpleDialogueEventRepository : DialogueEventRepository {
    override fun create(dialogueEvent: DialogueEvent) {
        println("Dialogue event occurred: $dialogueEvent")
    }
}