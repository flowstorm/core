package ai.flowstorm.core.runtime

import ai.flowstorm.common.messaging.MessageSender
import ai.flowstorm.core.Context
import ai.flowstorm.core.Pipeline
import ai.flowstorm.core.Request
import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.model.Profile
import ai.flowstorm.core.model.Session
import ai.flowstorm.core.model.Turn
import ai.flowstorm.core.repository.ProfileRepository
import ai.flowstorm.core.resources.CommunityResource
import ai.flowstorm.core.resources.DialogueEventResource
import ai.flowstorm.core.type.Dynamic
import ai.flowstorm.core.type.toLocation
import org.litote.kmongo.toId
import javax.inject.Inject

class ContextFactory {

    @Inject
    lateinit var communityResource: CommunityResource

    @Inject
    lateinit var dialogueEventResource: DialogueEventResource

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var messageSender: MessageSender

    fun createContext(pipeline: Pipeline, session: Session, request: Request, contextLog: ContextLog): Context {
        val userProfile = profileRepository.findBy(session.user._id, session.space_id)
            ?: Profile(user_id = session.user._id, space_id = session.space_id)
        val dialogue_id = session.dialogueStack.peek()?.id?.toId() ?: session.application.dialogue_id
        val context = Context(
            pipeline,
            userProfile,
            session,
            Turn(session_id = session._id, dialogue_id = dialogue_id, input = request.input).also {
                it.request.attributes = request.attributes
            },
            contextLog.logger,
            request.input.locale,
            communityResource,
            dialogueEventResource,
            messageSender
        )
        request.attributes.forEach {
            val name = if (it.key == "clientLocation")
                "clientUserLocation"
            else
                it.key
            val value = when (name) {
                "clientUserLocation" ->
                    (it.value as String).toLocation().apply {
                        session.location = this
                    }
                "clientTemperature", "clientAmbientLight", "clientSpatialMotion" ->
                    it.value.toString().toDouble() // can be integer or double
                else -> {
                    if ((it.value is String) && ((it.value as String).startsWith("lat=") || (it.value as String).startsWith("lng=")))
                        (it.value as String).toLocation()
                    else if (it.value is Map<*, *>)
                        Dynamic(it.value as Map<String, Any>)
                    else
                        it.value // type by JSON
                }
            }
            context.getAttributes(name)[AbstractDialogue.defaultNamespace].set(name, value)
        }
        return context
    }
}