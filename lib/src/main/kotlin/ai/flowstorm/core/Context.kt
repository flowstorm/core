package ai.flowstorm.core

import ai.flowstorm.common.messaging.MessageSender
import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.model.*
import ai.flowstorm.core.repository.CommunityRepository
import ai.flowstorm.core.repository.DialogueEventRepository
import ai.flowstorm.core.type.Attributes
import org.slf4j.Logger
import java.util.*

data class Context(
    var pipeline: Pipeline,
    val userProfile: Profile,
    val session: Session,
    val turn: Turn,
    val logger: Logger,
    var locale: Locale? = null,
    val communityRepository: CommunityRepository,
    val dialogueEventRepository: DialogueEventRepository,
    val messageSender: MessageSender,
    val communities: MutableMap<String, Community> = mutableMapOf(),
    var intentModels: List<Model> = listOf(),
    var dialogueEvent: DialogueEvent? = null,
    var sleepTimeout: Int = 0
) {
    // aliases
    val input get() = turn.input
    val user get() = session.user
    val attributes get() = turn.attributes
    val application get() = session.application
    val previousTurns get() = session.turns.reversed()
    val sessionEnded get() = session.dialogueStack.isEmpty()
    fun processPipeline() = pipeline.process(this)
    @Deprecated("Use turn.expectedPhrases instead", replaceWith = ReplaceWith("turn.expectedPhrases"))
    val expectedPhrases get() = turn.expectedPhrases

    fun getAttributes(name: String): Attributes =
        if (name.startsWith("user") || name.startsWith("clientUser"))
            userProfile.attributes
        else if (name.startsWith("turn") || name.startsWith("clientTurn"))
            turn.attributes
        else
            session.attributes

    fun getAttribute(name: String, namespace: String = AbstractDialogue.defaultNamespace) = getAttributes(name)[namespace][name]

    fun sendMessage(subject: String, text: String) = with (user) {
        messageSender.sendMessage(MessageSender.Recipient(username, "$name $surname"), subject, text)
    }

    fun createDialogueEvent(e: Throwable) {
        dialogueEvent = DialogueEvent(
            datetime = Date(),
            type = DialogueEvent.Type.ServerError,
            user = user,
            sessionId = session.sessionId,
            properties = session.properties,
            applicationName = application.name,
            dialogue_id = application.dialogue_id,
            //TODO Replace with actual node ID after node sequence is added in Context
            nodeId = 0,
            text = DialogueEvent.toText(e),
            space_id = session.space_id,
        ).also {
            dialogueEventRepository.create(it)
        }
    }
}