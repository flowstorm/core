package org.promethist.core

import java.util.*

interface Pipeline {

    val components: LinkedList<Component>

    fun process(context: Context): Context

    class PipelineComponentFailed(component: Component, cause: Throwable) : Throwable("Pipeline component failed: ${component::class.simpleName}", cause)
}