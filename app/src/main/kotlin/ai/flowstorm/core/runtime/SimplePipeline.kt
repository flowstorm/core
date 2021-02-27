package ai.flowstorm.core.runtime

import ai.flowstorm.core.Component
import ai.flowstorm.core.Context
import ai.flowstorm.core.Pipeline
import java.util.*

class SimplePipeline(override val components: LinkedList<Component>) : Pipeline {
    override fun process(context: Context): Context {
        var processedContext = context
        if (components.isNotEmpty())
            processedContext = components.pop().process(context)
        return processedContext
    }
}