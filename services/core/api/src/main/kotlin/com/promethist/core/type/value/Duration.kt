package com.promethist.core.type.value


data class Duration(val value: Float, val unit: String = "", val year: Float = 0.0F, val month: Float = 0.0F,
                    val week: Float, val day: Float = 0.0F, val hour: Float = 0.0F, val minute: Float = 0.0F,
                    val second: Float, val normalized: Amount = Amount("0".toBigDecimal(), "second")): Value()