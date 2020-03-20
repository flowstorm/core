package com.promethist.core.context

import com.promethist.core.model.*
import com.promethist.core.nlp.Context
import com.promethist.core.nlp.Input
import com.promethist.core.profile.ProfileRepository
import org.slf4j.LoggerFactory
import javax.inject.Inject

class ContextFactory {

    @Inject
    lateinit var profileRepository: ProfileRepository

    fun createContext(session: Session, message: Message): Context {
        val profile = profileRepository.find(session.user._id)
                ?: Profile(user_id = session.user._id, name = session.user.username)

        val input = Input(message.items.first().text!!)
        val turn = session.turns.lastOrNull()?.copy(input = input, responseItems = mutableListOf()) ?: Turn(input)

        return Context(
                profile,
                session,
                turn,
                LoggerFactory.getLogger(Context::class.java)
        )
    }
}