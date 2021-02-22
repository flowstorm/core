package org.promethist.core.context

import org.promethist.common.messaging.MessageSender
import org.promethist.core.Context
import org.promethist.core.Pipeline
import org.promethist.core.Request
import org.promethist.core.dialogue.AbstractDialogue
import org.promethist.core.model.Profile
import org.promethist.core.model.Session
import org.promethist.core.model.Turn
import org.promethist.core.repository.ProfileRepository
import org.promethist.core.resources.CommunityResource
import org.promethist.core.resources.DialogueEventResource
import org.promethist.core.runtime.DialogueLog
import org.promethist.core.type.Dynamic
import org.promethist.core.type.toLocation
import javax.inject.Inject

class ContextFactory {

    @Inject
    lateinit var communityResource: CommunityResource

    @Inject
    lateinit var dialogueEventResource: DialogueEventResource

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var dialogueLog: DialogueLog

    @Inject
    lateinit var messageSender: MessageSender

    fun createContext(pipeline: Pipeline, session: Session, request: Request): Context {
        val userProfile = profileRepository.findBy(session.user._id, session.space_id)
            ?: Profile(user_id = session.user._id, space_id = session.space_id)
        val context = Context(
            pipeline,
            userProfile,
            session,
            Turn(session_id = session._id, input = request.input).also {
                it.request.attributes = request.attributes
            },
            dialogueLog.logger,
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