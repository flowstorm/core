package com.promethist.core.builder

import com.promethist.common.RestClient
import com.promethist.core.builder.IntentModelBuilder.Output
import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.util.LoggerDelegate
import java.net.URL
import java.util.*
import javax.ws.rs.WebApplicationException

class IllusionistModelBuilder(val apiUrl: String, val apiKey: String) : IntentModelBuilder {

    private val logger by LoggerDelegate()

    override fun build(irModel: IntentModel, language: Locale, intents: List<AbstractDialogue.Intent>, , oodExamples: List<SourceCodeBuilder.GlobalIntent>) {
        build(irModel.id, irModel.name, language, intents)
    }

    override fun build(modelId: String, name: String, language: Locale, intents: List<AbstractDialogue.Intent>, , oodExamples: List<SourceCodeBuilder.GlobalIntent>) {
        val items = mutableMapOf<String, Output.Item>()
        intents.forEach { intent ->
            items[intent.name] = Output.Item(intent.utterances, intent.id.toString(), intent.threshold)
        }

        val oodStrings = mutableListOf<String>()
        oodExamples.forEach { intent -> oodStrings.addAll(intent.utterances) }

        items["OOD"] = Output.Item(oodStrings.toTypedArray(), "OOD", 0.0F)

        build(modelId, name, language, items)
    }

    override fun build(modelId: String, name: String, language: Locale, intents: Map<String, Output.Item>) {
        val output = Output(Output.Model(name, language.toString()), intents)

        val url = URL("$apiUrl/models/$modelId?key=$apiKey")
//        logger.info("$url < $output")
        try {
            RestClient.call<Any>(url, "POST", output = output)
        } catch (e: WebApplicationException) {
            RestClient.call<Any>(url, "PUT", output = output)
        }
        logger.info("built intent model name=$name, id=$modelId")
    }
}