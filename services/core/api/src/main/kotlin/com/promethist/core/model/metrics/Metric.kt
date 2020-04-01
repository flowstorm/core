package com.promethist.core.model.metrics

data class Metric(val namespace: String, val name: String, var value: Long = 0) {
    fun increment() = value++
}