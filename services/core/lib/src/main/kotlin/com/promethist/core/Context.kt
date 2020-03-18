package com.promethist.core

import com.promethist.core.model.Profile
import com.promethist.core.model.Turn
import com.promethist.core.model.Session
import org.slf4j.Logger

data class Context(val profile: Profile, val session: Session, val turn: Turn, val logger: Logger)