package org.promethist.core.builder

import org.promethist.common.RestClient
import org.promethist.core.builder.IntentModelBuilder.Output
import org.promethist.core.dialogue.AbstractDialogue
import org.promethist.core.model.DialogueSourceCode
import org.promethist.core.model.IntentModel
import org.promethist.util.LoggerDelegate
import java.net.URL
import java.util.*
import javax.ws.rs.WebApplicationException

class IllusionistModelBuilder(val apiUrl: String, val apiKey: String, val approach: String) : IntentModelBuilder {

    companion object {
        const val buildTimeout = 180000
        const val outOfDomainActionName = "outofdomain"
    }

    private val logger by LoggerDelegate()

    init {
        logger.info("Created with API URL $apiUrl (approach=$approach)")
    }

    override fun build(irModel: IntentModel, language: Locale, intents: List<AbstractDialogue.Intent>, oodExamples: List<DialogueSourceCode.GlobalIntent>) {
        build(irModel.id, irModel.name, language, intents, oodExamples)
    }

    override fun build(modelId: String, name: String, language: Locale, intents: List<AbstractDialogue.Intent>, oodExamples: List<DialogueSourceCode.GlobalIntent>) {
        val items = mutableMapOf<String, Output.Item>()
        intents.forEach { intent ->
            items[intent.name] = Output.Item(intent.utterances, intent.id.toString(), intent.threshold)
        }

        val oodStrings = mutableListOf<String>()
        oodExamples.forEach { intent -> oodStrings.addAll(intent.utterances) }

        if (approach == "logistic") {
            items[outOfDomainActionName] = Output.Item(oodStrings.toTypedArray(), outOfDomainActionName, 0.0F)
        }

        build(modelId, name, language, items)
    }

    override fun build(modelId: String, name: String, language: Locale, intents: Map<String, Output.Item>) {
        val output = Output(Output.Model(name, language.toString(), approach = approach), intents)

        val url = URL("$apiUrl/models/$modelId?key=$apiKey")
//        logger.info("$url < $output")
        try {
            RestClient.call<Any>(url, "POST", output = output, timeout = buildTimeout)
        } catch (e: WebApplicationException) {
            RestClient.call<Any>(url, "PUT", output = output, timeout = buildTimeout)
        }
        logger.info("Built intent model name=$name, id=$modelId")
    }
}