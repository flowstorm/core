package ai.flowstorm.core.builder

import ai.flowstorm.common.RestClient
import ai.flowstorm.common.ServiceUrlResolver
import ai.flowstorm.core.builder.IntentModelBuilder.Output
import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.model.IntentModel
import ai.flowstorm.util.LoggerDelegate
import java.net.URL
import java.util.*
import javax.ws.rs.WebApplicationException

class IllusionistModelBuilder(val apiUrl: String, val apiKey: String, val approach: String) : IntentModelBuilder, EntityModelBuilder {

    companion object {
        const val buildTimeout = 180000
        const val outOfDomainActionName = "outofdomain"
    }

    private val logger by LoggerDelegate()
    private val url by lazy {
        (if (apiUrl.startsWith("http://localhost"))
            ServiceUrlResolver.getEndpointUrl("illusionist-training")
        else
            apiUrl
        ) + "/training"
    }

    init {
        logger.info("Created with API URL $url (approach=$approach)")
    }

    override fun build(irModel: IntentModel, language: Locale, intents: List<AbstractDialogue.Intent>, oodExamples: List<String>) {
        build(irModel.id, irModel.name, language, intents, oodExamples)
    }

    override fun build(modelId: String, name: String, language: Locale, intents: List<AbstractDialogue.Intent>, oodExamples: List<String>) {
        val items = mutableMapOf<String, Output.Item>()
        intents.forEach { intent ->
            items[intent.name] = Output.Item(intent.utterances, intent.id.toString(), intent.threshold)
        }

        if (approach == "logistic") {
            items[outOfDomainActionName] = Output.Item(oodExamples.toTypedArray(), outOfDomainActionName, 0.0F)
        }

        build(modelId, name, language, items)
    }

    override fun build(modelId: String, name: String, language: Locale, intents: Map<String, Output.Item>) {
        val output = Output(Output.Model(name, language.toString(), approach = approach), intents)

        val url = URL("$url/models/$modelId?key=$apiKey")
//        logger.info("$url < $output")
        try {
            RestClient.call<Any>(url, "POST", output = output, timeout = buildTimeout)
        } catch (e: WebApplicationException) {
            RestClient.call<Any>(url, "PUT", output = output, timeout = buildTimeout)
        }
        logger.info("Built intent model name=$name, id=$modelId")
    }

    override fun trainEntityModel(dataset: EntityDataset) {
        val url = URL("$url/entity/${dataset._id}?space_id=${dataset.space_id}&language=${dataset.language}" )
        RestClient.call<Any>(url, "PUT", output = dataset, timeout = buildTimeout)
    }
}