package com.promethist.core.context

import com.promethist.core.Context
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.resources.SessionResource
import javax.inject.Inject

class ContextPersister {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var sessionResource: SessionResource

    fun persist(context: Context) {
        context.session.turns.add(context.turn)
        sessionResource.update(context.session)
        profileRepository.save(context.profile)
    }
}