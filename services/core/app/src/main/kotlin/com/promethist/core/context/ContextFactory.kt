package com.promethist.core.context

import com.promethist.core.model.*
import com.promethist.core.Context
import com.promethist.core.Pipeline
import com.promethist.core.Request
import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.dialogue.attribute.ContextualAttributeDelegate
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.resources.CommunityResource
import com.promethist.core.runtime.DialogueLog
import com.promethist.core.type.Dynamic
import com.promethist.core.type.Memory
import com.promethist.core.type.toLocation
import javax.inject.Inject

class ContextFactory {

    @Inject
    lateinit var communityResource: CommunityResource

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var dialogueLog: DialogueLog

    fun createContext(pipeline: Pipeline, session: Session, request: Request): Context {
        val userProfile = profileRepository.find(session.user._id)
                ?: Profile(user_id = session.user._id)
        val context = Context(
                pipeline,
                userProfile,
                session,
                Turn(input = request.input),
                dialogueLog.logger,
                request.input.locale,
                communityResource
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