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
        context.session.turns.add(context.turn)
        context.session.metrics.apply {
            clear()
            addAll(context.metrics.metrics)
        }

        context.session.log.addAll(dialogueLog.log)
        sessionResource.update(context.session)
        profileRepository.save(context.profile)
    }
}