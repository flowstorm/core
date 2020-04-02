package com.promethist.core.builder

import com.promethist.common.ObjectUtil
import com.promethist.common.RestClient
import com.promethist.core.builder.IntentModelBuilder.Output
import com.promethist.core.Dialogue
import com.promethist.util.LoggerDelegate
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*
import javax.ws.rs.WebApplicationException

class IllusionistModelBuilder(val apiUrl: String, val apiKey: String) : IntentModelBuilder {

    private val logger by LoggerDelegate()

    override fun build(modelId: String, name: String, language: Locale, intents: List<Dialogue.Intent>) {
        val items = mutableMapOf<String, Output.Item>()
        intents.forEach { intent ->
            items[intent.name] = Output.Item(intent.utterances, intent.id.toString())
        }

        build(modelId, name, language, items)
    }

    override fun build(modelId: String, name: String, language: Locale, intents: Map<String, Output.Item>) {
        val output = Output(Output.Model(name, language.toString()), intents)

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