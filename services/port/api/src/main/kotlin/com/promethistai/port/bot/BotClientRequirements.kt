package com.promethistai.port.bot

import java.io.Serializable

data class BotClientRequirements(
    var ssml: Boolean = false) : Serializable