package com.promethist.core.model

data class Context(
        var userId: String? = null,
        var sessionId: String? = null,
        var message: String
       // @Transient var session: Session
        //TODO move from helena
) {
    lateinit var session: Session
}