package com.promethistai.port.model

data class Contract(
    val key: String,
    var bot: String,
    var botKey: String?,
    var model: String?,
    var remoteEndpoint: String?
)