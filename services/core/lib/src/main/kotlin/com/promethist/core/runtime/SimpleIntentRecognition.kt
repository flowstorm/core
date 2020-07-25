package com.promethist.core.runtime

import com.promethist.core.Component
import com.promethist.core.Context
import com.promethist.core.Input
import com.promethist.core.dialogue.Dialogue
import com.promethist.core.model.IrModel

class SimpleIntentRecognition : Component {

    lateinit var models: Map<IrModel, Map<Int, List<String>>>

    override fun process(context: Context): Context = Dialogue.run.let {
        if (!this::models.isInitialized) {
            initModels(it.node.dialogue)
        }

        val text = context.input.transcript.text

        // select requested models
        val requestedModels = models.filter { it.key.id in context.irModels.map { it.id } }
        //merge models
        val mergedModels = requestedModels.map { it.value }.reduce { acc, map -> acc + map }
        //find matching intent ids
        val intentIds = mergedModels.filter { it.value.filter { it.contains(text, true) }.isNotEmpty() }.keys

        val intentId = when (intentIds.size) {
            0 -> error("no intent model ${context.irModels.map { it.name }} matching text \"$text\"")
            1 -> intentIds.first()
            else -> {
                context.logger.warn("multiple intents $intentIds matched text \"$text\"")
                intentIds.first()
            }
        }

        context.turn.input.classes.add(Input.Class(Input.Class.Type.Intent, intentId.toString()))

        return context
    }

    private fun initModels(dialogue: Dialogue) {
        val map = mutableMapOf<IrModel, Map<Int, List<String>>>()

        map.put(com.promethist.core.builder.IrModel(dialogue.buildId, dialogue.dialogueName, null),
                dialogue.globalIntents.map { it.id to it.utterances.toList() }.toMap())

        dialogue.userInputs.forEach {
            map.put(com.promethist.core.builder.IrModel(dialogue.buildId, dialogue.dialogueName, it.id),
                    it.intents.map { it.id to it.utterances.toList() }.toMap())
        }

        models = map.toMap()
    }
}