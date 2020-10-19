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
            val attribute = Memory(when (it.key) {
                "clientLocation" ->
                    (it.value as String).toLocation().apply {
                        session.location = this
                    }
                "clientTemperature", "clientAmbientLight", "clientSpatialMotion" ->
                    it.value.toString().toDouble() // can be integer or double
                else -> {
                    if (it.key.endsWith("Location"))
                        (it.value as String).toLocation()
                    else
                        it.value
                }
            })
            (if (ContextualAttributeDelegate.isClientUserAttribute(it.key))
                userProfile.attributes
            else
                session.attributes
            )[AbstractDialogue.defaultNamespace][it.key] = attribute
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