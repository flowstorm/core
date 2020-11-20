package com.promethist.core.dialogue.metric

import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.model.metrics.Metric
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MetricDelegate(private val metricSpec: String) : ReadWriteProperty<AbstractDialogue, Long> {

    init {
        val r = "(?<namespace>[A-Za-z0-9_]*)\\.(?<name>[A-Za-z0-9_]*)".toRegex()
        require(r.matches(metricSpec)) { "Bad metric specification." }
    }

    val namespace: String = metricSpec.substringBefore(".")
    val name = metricSpec.substringAfter(".")

    private val metric
        get() = with(AbstractDialogue.run.context.session) {
            metrics.firstOrNull { it.namespace == namespace && it.name == name }
                    ?: Metric(namespace, name).also { metrics.add(it) }
        }

    override operator fun getValue(thisRef: AbstractDialogue, property: KProperty<*>): Long = metric.value

    override operator fun setValue(thisRef: AbstractDialogue, property: KProperty<*>, value: Long) {
        metric.value = value
    }
}