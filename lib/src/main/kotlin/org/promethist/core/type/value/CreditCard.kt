package org.promethist.core.type.value

data class CreditCard(val value: String, val issuer: String = ""): Value()