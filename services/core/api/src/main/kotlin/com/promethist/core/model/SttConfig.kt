package com.promethist.core.model

import java.io.Serializable
import java.time.ZoneId
import java.util.*

// todo remove lang
data class SttConfig(val locale: Locale, val zoneId: ZoneId, val sampleRate: Int = 0, val encoding: Encoding = Encoding.LINEAR16, val mode: Mode = Mode.Default): Serializable {
    enum class Encoding { LINEAR16, MULAW }
    enum class Mode { Default, SingleUtterance, Duplex }
}
