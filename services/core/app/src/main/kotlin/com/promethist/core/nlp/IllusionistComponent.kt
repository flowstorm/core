package com.promethist.core.nlp

import com.fasterxml.jackson.annotation.JsonProperty
import com.promethist.core.model.Turn
import org.jetbrains.kotlin.daemon.common.toHexString
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType

class IllusionistComponent : Component {

    @Inject
    lateinit var webTarget: WebTarget

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun process(context: Context): Context {
        if (context.turn.dialogueStack.isEmpty()) {
            logger.info("Illusionist NLP adapter - nothing to do.")
            return context
        }

        val request = Request(context.input.text, getModelsIds(context.turn.dialogueStack.first))
        val responses = webTarget.path("/multi_model").request().post(Entity.json(request), object : GenericType<List<Response>>() {})

        for (response in responses) {
            context.turn.input.classes.add(Input.Class(Input.Class.Type.Intent, response.answer, response.confidence))
        }

        return context
    }

    private fun getModelsIds(frame: Turn.DialogueStackFrame): List<String> = listOf(
            md5(frame.name), md5(frame.name + frame.nodeId)
    )

    private fun md5(string: String) = MessageDigest.getInstance("MD5").digest(string.toByteArray()).toHexString()

    data class Response(
            val _id: String,
            val answer: String,
            val confidence: Float,
            val hit: String
    )

    data class Request(
            val query: String,
            val _ids: List<String>,
            @field:JsonProperty("denied_answers")
            val deniedAnswers: List<List<String?>?> = listOf(),
            @field:JsonProperty("allowed_answers")
            val allowedAnswers: List<List<String?>?> = listOf(),
            val n: Int = 3 //number of results ordered by confidence
    )
}