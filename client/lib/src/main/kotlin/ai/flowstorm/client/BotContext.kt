package ai.flowstorm.client

import ai.flowstorm.core.Defaults
import ai.flowstorm.core.model.Voice
import ai.flowstorm.core.type.MutablePropertyMap
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