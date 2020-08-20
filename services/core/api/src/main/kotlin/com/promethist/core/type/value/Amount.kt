package com.promethist.core.type.value

import java.math.BigDecimal

open class Amount(open val value: BigDecimal, open val unit: String = ""): Value()