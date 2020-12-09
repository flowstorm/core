package org.promethist.core

import com.fasterxml.jackson.annotation.JsonAlias
import org.promethist.core.type.PropertyMap

data class Request(
    val appKey: String,
    @JsonAlias("sender")
    val deviceId: String,
    val token: String? = null,
    val sessionId: String,
    val initiationId: String? = null,
    val input: Input,
    var attributes: PropertyMap
)