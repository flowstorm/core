package org.promethist.core.runtime

import org.promethist.core.Component
import org.promethist.core.Context
import org.promethist.core.Pipeline
import java.util.*

class SimplePipeline(override val components: LinkedList<Component>) : Pipeline {
    override fun process(context: Context): Context {
        var processedContext = context
        if (components.isNotEmpty())
            processedContext = components.pop().process(context)
        return processedContext
    }
}