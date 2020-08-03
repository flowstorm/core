package com.promethist.client

import com.promethist.core.Defaults
import com.promethist.core.model.Voice
import com.promethist.core.type.Dynamic
import com.promethist.core.type.PropertyMap
import java.time.ZoneId
import java.util.*

data class BotContext(
        var url: String,
        var key: String,
        var sender: String,
        val attributes: PropertyMap,
        var introText: String = "#intro",
        var token: String? = null,
        var voice: Voice? = null,
        var autoStart: Boolean = true,
        var sessionId: String? = null,
        var locale: Locale = Defaults.locale,
        var zoneId: ZoneId = ZoneId.systemDefault()
)