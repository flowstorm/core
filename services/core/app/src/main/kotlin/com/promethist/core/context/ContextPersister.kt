package com.promethist.core.context

import com.promethist.core.Context
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.resources.SessionResource
import com.promethist.core.runtime.DialogueLog
import javax.inject.Inject

class ContextPersister {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var sessionResource: SessionResource

    @Inject
    lateinit var dialogueLog: DialogueLog

    fun persist(context: Context) {
        context.turn.log.addAll(dialogueLog.log)
        context.session.turns.add(context.turn)

        sessionResource.update(context.session)
        profileRepository.save(context.userProfile)
    }
}