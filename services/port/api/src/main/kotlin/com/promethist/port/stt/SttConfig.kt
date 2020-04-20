package com.promethist.port.stt

import java.io.Serializable
import java.time.ZoneId
import java.util.*

// todo remove lang
data class SttConfig(val locale: Locale, val zoneId: ZoneId, val sampleRate: Int = 0): Serializable
