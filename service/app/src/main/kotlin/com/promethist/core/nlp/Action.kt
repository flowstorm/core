package com.promethist.core.nlp

import com.promethist.core.Component
import com.promethist.core.Context

class Action : Component {
    override fun process(context: Context): Context {
        var text = context.input.transcript.text

        if (text == "\$intro") text = "#intro" //backward compatibility

        if (text.matches("$ACTION_PREFIX([\\w\\-]+)".toRegex())) {
            context.input.action  = text.substringAfter(ACTION_PREFIX)
            return context //do not process the rest of pipeline
        }

        return context.pipeline.process(context)
    }

    companion object {
        const val ACTION_PREFIX = "#"
    }
}