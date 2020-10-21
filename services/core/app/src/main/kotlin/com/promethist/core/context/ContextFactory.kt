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

        request.attributes.forEach {
            val name = if (it.key == "clientLocation")
                "clientUserLocation"
            else
                it.key
            val attribute = Memory(when (name) {
                "clientUserLocation" ->
                    (it.value as String).toLocation().apply {
                        session.location = this
                    }
                "clientTemperature", "clientAmbientLight", "clientSpatialMotion" ->
                    it.value.toString().toDouble() // can be integer or double
                else -> {
                    if (name.endsWith("Location"))
                        (it.value as String).toLocation()
                    else
                        it.value
                }
            })
            (if (ContextualAttributeDelegate.isClientUserAttribute(name))
                userProfile.attributes
            else
                session.attributes
            )[AbstractDialogue.defaultNamespace][name] = attribute
        }
        return Context(
                pipeline,
                userProfile,
                session,
                Turn(input = request.input),
                dialogueLog.logger,
                request.input.locale,
                communityResource
        )
    }
}