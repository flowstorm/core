package org.promethist.core.context

import org.promethist.core.model.*
import org.promethist.core.Context
import org.promethist.core.Pipeline
import org.promethist.core.Request
import org.promethist.core.dialogue.AbstractDialogue
import org.promethist.core.profile.ProfileRepository
import org.promethist.core.resources.CommunityResource
import org.promethist.core.runtime.DialogueLog
import org.promethist.core.type.Dynamic
import org.promethist.core.type.toLocation
import org.promethist.common.messaging.MessageSender
import javax.inject.Inject

class ContextFactory {

    @Inject
    lateinit var communityResource: CommunityResource

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var dialogueLog: DialogueLog

    @Inject
    lateinit var messageSender: MessageSender

    fun createContext(pipeline: Pipeline, session: Session, request: Request): Context {
        val userProfile = profileRepository.find(session.user._id)
                ?: Profile(user_id = session.user._id)
        val context = Context(
                pipeline,
                userProfile,
                session,
                Turn(input = request.input).also {
                    it.request.attributes = request.attributes
                },
                dialogueLog.logger,
                request.input.locale,
                communityResource,
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