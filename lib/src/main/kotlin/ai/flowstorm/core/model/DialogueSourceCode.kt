package ai.flowstorm.core.model

import ai.flowstorm.core.type.PropertyMap

open class DialogueSourceCode(
    override val dialogueId: String,
    override val dialogueName: String,
    val className: String,
    val version: Int,
    val buildId: String,
    val parameters: PropertyMap,
    val code: String
) : DialogueModel {

    interface Node

    data class Intent(val nodeId: Int, val nodeName: String, val threshold: Float, val utterances: List<String>, val entities: List<String>) : Node
    data class GlobalIntent(val nodeId: Int, val nodeName: String, val threshold: Float,  val utterances: List<String>, val entities: List<String>) : Node
    data class UserInput(val nodeId: Int, val nodeName: String, val intentNames: List<String>, val actionNames: List<String>, val sttMode: SttConfig.Mode? = null, val skipGlobalIntents: Boolean, val transitions: Map<String, String>, val expectedPhrases: CharSequence = "", val code: CharSequence = "") : Node
    data class Speech(val nodeId: Int, val nodeName: String, val background: String? = null, val ttsConfig: TtsConfig? = null, val repeatable: Boolean, val texts: List<String>) : Node
    data class Sound(val nodeId: Int, val nodeName: String, val source: String? = null, val repeatable: Boolean) : Node
    data class Image(val nodeId: Int, val nodeName: String, val source: String? = null) : Node
    data class Command(val nodeId: Int, val nodeName: String, val command: String, val code: CharSequence) : Node
    data class Function(val nodeId: Int, val nodeName: String, val transitions: Map<String, String>, val code: CharSequence) : Node
    data class SubDialogue(val nodeId: Int, val nodeName: String, val subDialogueId: String, val code: CharSequence = "") : Node
    data class GoBack(val nodeId: Int, val nodeName: String, val repeat: Boolean) : Node
    data class Sleep(val nodeId: Int, val nodeName: String, val timeout: Int) : Node
    data class Action(val nodeId: Int, val nodeName: String, val action: String) : Node
    data class GlobalAction(val nodeId: Int, val nodeName: String, val action: String) : Node
}