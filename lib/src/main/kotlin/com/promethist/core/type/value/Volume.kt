package com.promethist.core.type.value

import com.promethist.core.type.Decimal

data class Volume(override val value: Decimal, override val unit: String) : Amount(value, unit)