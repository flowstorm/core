package ai.flowstorm.core.type.value

import ai.flowstorm.core.type.Decimal

data class Distance(override val value: Decimal, override val unit: String) : Amount(value, unit)