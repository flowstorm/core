package ai.flowstorm.core.type.value

import ai.flowstorm.core.type.DateTime

data class GrainedTime(val value: DateTime, val grain: String = ""): Time()