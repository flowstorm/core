package org.promethist.core.type.value

import org.promethist.core.type.DateTime

data class GrainedTime(val value: DateTime, val grain: String = ""): Time()