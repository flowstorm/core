package com.promethist.core.context

import com.promethist.core.model.*
import com.promethist.core.Context
import com.promethist.core.Input
import com.promethist.core.Pipeline
import com.promethist.core.model.metrics.Metrics
import com.promethist.core.profile.ProfileRepository
import org.slf4j.LoggerFactory
import javax.inject.Inject

class ContextFactory {

    @Inject
    lateinit var profileRepository: ProfileRepository

    fun createContext(pipeline: Pipeline, session: Session, input: Input): Context {
        val profile = profileRepository.find(session.user._id)
                ?: Profile(user_id = session.user._id, name = session.user.username)

        val turn = session.turns.lastOrNull()?.copy(input = input, responseItems = mutableListOf()) ?: Turn(input)

        return Context(
                pipeline,
                profile,
                session,
                turn,
                Metrics(session.metrics),
                LoggerFactory.getLogger(Context::class.java)
        )
    }
}