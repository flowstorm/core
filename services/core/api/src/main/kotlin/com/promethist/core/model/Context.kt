package com.promethist.core.model

data class Context(
        var userId: String? = null,
        var sessionId: String? = null,
        var message: String
        //TODO move from helena
)