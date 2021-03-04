package ai.flowstorm.core.nlp

import org.glassfish.hk2.api.IterableProvider
import ai.flowstorm.core.Component
import ai.flowstorm.core.Context
import ai.flowstorm.core.type.DucklingEntity
import ai.flowstorm.util.LoggerDelegate
import java.util.*
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Form
import javax.ws.rs.core.GenericType

class Duckling: Component {
    @Inject
    lateinit var webTargets: IterableProvider<WebTarget>

    val webTarget: WebTarget get() = webTargets.named("duckling").get()

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {
        logger.info("Calling Duckling with input ${context.input}")

        try {
            val response = webTarget.request().post(
                    Entity.form(Form()
                            .param("locale", getValidLocale(context.input.locale))
                            .param("tz", context.input.zoneId.id)
                            .param("text", context.input.transcript.text)), object : GenericType<List<DucklingEntity>>() {})
            for (entity in response) {
                context.turn.input.entityMap.getOrPut(entity.className, { mutableListOf() }).add(entity)
            }
        } catch (t:Throwable) {
            // The exception should not block pipeline processing
            context.logger.error("Call to Duckling failed: " + t.message)
        }
        return context.pipeline.process(context)
    }

    private fun getValidLocale(locale: Locale): String {
        return when (locale.toLanguageTag()){
            "cs" -> "cs_CZ"
            "en" -> "en_US"
            else -> locale.toLanguageTag()
        }
    }
}