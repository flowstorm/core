package com.promethist.core.context

import com.promethist.core.model.*
import com.promethist.core.Context
import com.promethist.core.Input
import com.promethist.core.Pipeline
import com.promethist.core.model.metrics.Metrics
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.runtime.DialogueLog
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject

class ContextFactory {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var dialogueLog: DialogueLog

    fun createContext(pipeline: Pipeline, session: Session, input: Input): Context {
        val profile = profileRepository.find(session.user._id)
                ?: Profile(user_id = session.user._id, name = session.user.username)

        return Context(
                pipeline,
                profile,
                session,
                Turn(input = input),
                Metrics(session.metrics),
                dialogueLog.logger
        )
    }
}