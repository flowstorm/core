package org.promethist.core.type.value

import org.promethist.core.type.Decimal

data class Currency(override val value: Decimal, override val unit: String) : Amount(value, unit)