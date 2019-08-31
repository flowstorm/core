package com.promethistai.port.model

data class Contract(
    var name: String? = null,
    var key: String? = null,
    var bot: String? = null,
    var botKey: String? = null,
    var model: String? = null,
    var remoteEndpoint: String? = null
)