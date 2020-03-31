package com.promethist.core.type

open class Value<V>(open var value: V) {

    override fun equals(other: Any?): Boolean = if (other is Value<*>) (value == other.value) else false

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "(value=$value)"
}