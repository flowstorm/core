package ai.flowstorm.core.builder

import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.model.DialogueSourceCode
import ai.flowstorm.core.model.IntentModel
import java.util.*

interface IntentModelBuilder {

    data class Output(val model: Model, val qa: Map<String, Item> = mapOf()) {
        data class Model(val name: String, val lang: String, val algorithm: String? = "FastTextSW", val approach: String? = "logistic")
        data class Item(val questions: Array<out String>, val answer: String, val threshold: Float)
    }

    fun build(modelId: String, name: String, language: Locale, intents: List<AbstractDialogue.Intent>, oodExamples: List<String>)

    fun build(irModel: IntentModel, language: Locale, intents: List<AbstractDialogue.Intent>, oodExamples: List<String>)

    fun build(modelId: String, name: String, language: Locale, intents: Map<String, Output.Item>)
}