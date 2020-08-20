package com.promethist.core.type.value

import java.math.BigDecimal

data class Volume(override val value: BigDecimal, override val unit: String) : Amount(value, unit)