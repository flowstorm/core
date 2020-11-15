package com.promethist.core.type.value

import com.promethist.core.type.Decimal

open class Amount(open val value: Decimal, open val unit: String = ""): Value() {
    override fun toString(): String {
        return "Amount(value=$value, unit=$unit)"
    }
}