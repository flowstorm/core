package com.promethist.core.model.metrics

import com.promethist.core.type.Value

/**
 * Class exposing metrics API to DialogueScript
 */
class Metrics(metrics: List<Metric>) {

    val metrics
        get() = metricMap.map { it.value }

    private var metricMap = metrics.associateBy { "{$it.namespace}.{$it.name}" }.toMutableMap()

    private val r = "(?<namespace>[A-Za-z0-9_]*)\\.(?<name>[A-Za-z0-9_]*)".toRegex()

    operator fun invoke(metricSpec: String, lambda: Value<Long>.() -> Unit = {}): Long {
        val metric = getMetric(metricSpec)

        val value = Value(metric.value)
        value.lambda()
        metric.value = value.value

        return metric.value
    }

    private fun getMetric(metricSpec: String): Metric = metricMap.getOrPut(metricSpec) {
        require(r.matches(metricSpec)) { "Bad metric specification." }
        val groups = r.matchEntire(metricSpec)!!.groups

        val namespace = groups[1]!!.value
        val name = groups[2]!!.value
        Metric(namespace, name, 0)
    }
}