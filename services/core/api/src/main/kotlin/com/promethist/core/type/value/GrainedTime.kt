package com.promethist.core.type.value

import com.promethist.core.type.DateTime

data class GrainedTime(val value: DateTime, val grain: String = ""): Time()