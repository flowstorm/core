package org.promethist.client

import org.promethist.core.Defaults
import org.promethist.core.model.Voice
import org.promethist.core.type.MutablePropertyMap
import java.time.ZoneId
import java.util.*

data class BotContext(
        var url: String,
        var key: String,
        var deviceId: String,
        val attributes: MutablePropertyMap,
        var introText: String = "#intro",
        var token: String? = null,
        var voice: Voice? = null,
        var autoStart: Boolean = true,
        var sessionId: String? = null,
        var initiationId: String? = null,
        var locale: Locale = Defaults.locale,
        var zoneId: ZoneId = ZoneId.systemDefault()
)