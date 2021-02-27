package ai.flowstorm.core.model

interface DialogueModel {

    companion object {
        const val defaultNamespace: String = "_default"
    }

    open val dialogueId: String
    open val dialogueName: String
}