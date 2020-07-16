package com.promethist.core.runtime

import com.promethist.core.Component
import com.promethist.core.Context

class Command : Component {
    override fun process(context: Context): Context {
        var text = context.input.transcript.text

        if (text == "\$intro") text = "#intro" //backward compatibility

        if (text.matches("$COMMAND_PREFIX([\\w\\-]+)".toRegex())) {
            context.input.command  = text.substringAfter(COMMAND_PREFIX)
            return context //do not process the rest of pipeline
        }

        return context.pipeline.process(context)
    }

    companion object {
        const val COMMAND_PREFIX = "#"
    }
}