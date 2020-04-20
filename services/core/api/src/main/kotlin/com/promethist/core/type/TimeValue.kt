package com.promethist.core.type

import java.time.ZonedDateTime

open class TimeValue<V>(override var value: V, open val time: ZonedDateTime): Value<V>(value) {

    override fun toString(): String = "${this::class.simpleName}(value=$value, time=$time)"
}