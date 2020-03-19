package com.promethist.core.context

import com.promethist.core.Context
import com.promethist.core.model.Profile
import com.promethist.core.model.Session
import com.promethist.core.model.Message
import com.promethist.core.model.Turn
import com.promethist.core.profile.ProfileRepository
import org.slf4j.LoggerFactory
import javax.inject.Inject

class ContextFactory {

    @Inject
    lateinit var profileRepository: ProfileRepository

    fun createContext(session: Session, message: Message): Context {
        val profile = profileRepository.find(session.user._id)
                ?: Profile(user_id = session.user._id, name = session.user.username)

        //TODO what to do when we have more then one message?? Helena process them like individual turns
        val turn = Turn(Turn.Input(message.items.first().text!!))

        return Context(
                profile,
                session,
                turn,
                LoggerFactory.getLogger(Context::class.java)
        )
    }
}