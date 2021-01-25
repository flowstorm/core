package org.promethist.core.runtime

import org.promethist.core.model.DialogueEvent
import org.promethist.core.repository.DialogueEventRepository

class SimpleDialogueEventRepository : DialogueEventRepository {
    override fun create(dialogueEvent: DialogueEvent) {
        println("Dialogue event occurred: $dialogueEvent")
    }
}