package com.promethist.core.builder

import com.promethist.core.dialogue.Dialogue
import java.util.*

interface IntentModelBuilder {

    data class Output(val model: Model, val qa: Map<String, Item> = mapOf()) {
        data class Model(val name: String, val lang: String/*, val algorithm: String? = null*/)
        data class Item(val questions: Array<out String>, val answer: String, val threshold: Float)
    }

    fun build(modelId: String, name: String, language: Locale, intents: List<Dialogue.Intent>)

    fun build(irModel: IrModel, language: Locale, intents: List<Dialogue.Intent>)

    fun build(modelId: String, name: String, language: Locale, intents: Map<String, Output.Item>)
}