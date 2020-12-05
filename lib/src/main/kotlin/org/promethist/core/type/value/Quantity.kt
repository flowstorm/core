package org.promethist.core.type.value

data class Quantity(val value: Float, val product: String = "", val unit: String = ""): Value()