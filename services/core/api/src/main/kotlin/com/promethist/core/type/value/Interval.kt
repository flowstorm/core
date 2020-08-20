package com.promethist.core.type.value

data class Interval(val from: GrainedTime, val to: GrainedTime): Time()