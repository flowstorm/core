package ai.flowstorm.core.type.value

import ai.flowstorm.core.type.Decimal

open class Amount(open val value: Decimal, open val unit: String = ""): Value() {
    override fun toString(): String {
        return "Amount(value=$value, unit=$unit)"
    }
}