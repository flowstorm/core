package com.promethist.core.runtime

import com.promethist.core.Component
import com.promethist.core.Context
import com.promethist.core.Pipeline
import java.util.*

class SimplePipeline(override val components: LinkedList<Component>) : Pipeline {
    override fun process(context: Context): Context {
        var processedContext = context
        if (components.isNotEmpty())
            processedContext = components.pop().process(context)
        return processedContext
    }
}