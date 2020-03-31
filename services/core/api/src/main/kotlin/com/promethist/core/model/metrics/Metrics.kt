package com.promethist.core.model.metrics

import com.promethist.core.model.Session
import com.promethist.core.type.Value

class Metrics(metrics: List<Session.Metric>) {
    private var metricMap = metrics.associateBy { "{$it.namespace}.{$it.name}" }.toMutableMap()

    private val r = "(?<namespace>[A-Za-z0-9_]*)\\.(?<name>[A-Za-z0-9_]*)".toRegex()

    operator fun invoke(metricSpec: String, lambda: Value<Long>.() -> Unit = {}): Long {
        require(r.matches(metricSpec)) { "Bad metric specification." }

        val metric = getMetric(metricSpec)

        val value = Value(metric.value)
        value.lambda()
        metric.value = value.value

        return metric.value
    }

    private fun getMetric(metricSpec: String): Session.Metric = metricMap.getOrPut(metricSpec) {
        val groups = r.matchEntire(metricSpec)!!.groups

        val namespace = groups[1]!!.value
        val name = groups[2]!!.value
        Session.Metric(namespace, name, 0)
    }
}