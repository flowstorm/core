package com.promethist.core.builder

import com.promethist.common.ObjectUtil
import com.promethist.common.RestClient
import com.promethist.core.nlp.Dialogue
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*
import javax.ws.rs.WebApplicationException

class IllusionistModelBuilder(val apiUrl: String, val apiKey: String) : IntentModelBuilder {

    data class Output(val model: Model, val qa: MutableMap<String, Item> = mutableMapOf()) {
        data class Model(val name: String, val lang: String/*, val algorithm: String? = null*/)
        data class Item(val questions: Array<out String>, val answer: String)
    }
    private var logger = LoggerFactory.getLogger(this::class.qualifiedName)

    override fun build(modelId: String, name: String, language: Locale, intents: List<Dialogue.Intent>) {
        val output = Output(Output.Model(name, language.toString()))
        intents.forEach { intent ->
            output.qa[intent.name] = Output.Item(intent.utterances, intent.id.toString())
        }
        val url = URL("$apiUrl/models/$modelId?key=$apiKey")
        logger.info("$url < $output")
        try {
            RestClient.call(url, "POST", output = output)
        } catch (e: WebApplicationException) {
            RestClient.call(url, "PUT", output = output)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val output = Output(Output.Model("modelX", "en"), mutableMapOf(
                    "intent1" to Output.Item(arrayOf("yes", "ok"), "-1"),
                    "intent2" to Output.Item(arrayOf("no", "nope"), "-2")
            ))
            println(ObjectUtil.defaultMapper.writeValueAsString(output))
        }
    }
}