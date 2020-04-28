package com.promethist.core.dialogue

import com.promethist.core.model.metrics.Metric
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MetricDelegate(private val metricSpec: String) : ReadWriteProperty<Dialogue, Long> {

    init {
        val r = "(?<namespace>[A-Za-z0-9_]*)\\.(?<name>[A-Za-z0-9_]*)".toRegex()
        require(r.matches(metricSpec)) { "Bad metric specification." }
    }

    val namespace: String = metricSpec.substringBefore(".")
    val name = metricSpec.substringAfter(".")

    private val metric
        get() = with(Dialogue.threadContext().context.session) {
            metrics.firstOrNull { it.namespace == namespace && it.name == name }
                    ?: Metric(namespace, name).also { metrics.add(it) }
        }

    override operator fun getValue(thisRef: Dialogue, property: KProperty<*>): Long = metric.value

    override operator fun setValue(thisRef: Dialogue, property: KProperty<*>, value: Long) {
        metric.value = value
    }
}