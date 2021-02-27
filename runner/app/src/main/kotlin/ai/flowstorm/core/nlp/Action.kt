package ai.flowstorm.core.nlp

import ai.flowstorm.core.Component
import ai.flowstorm.core.Context
import kotlin.reflect.full.createType

class Action : Component {
    override fun process(context: Context): Context {
        var text = context.input.transcript.text

        if (text == "\$intro") text = "#intro" //backward compatibility

        if (text.matches("$ACTION_PREFIX([\\w\\-]+)".toRegex())) {
            context.input.action  = text.substringAfter(ACTION_PREFIX)

            //when action is detected, we remove illusionist from pipeline
            context.pipeline.removeComponent(Illusionist::class.createType())

            return context
        }

        return context.pipeline.process(context)
    }

    companion object {
        const val ACTION_PREFIX = "#"
    }
}