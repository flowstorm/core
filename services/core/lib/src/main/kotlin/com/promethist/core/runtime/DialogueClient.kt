package com.promethist.core.runtime

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.common.ObjectUtil
import com.promethist.common.RestClient
import com.promethist.core.Component
import com.promethist.core.Context
import com.promethist.core.model.*
import com.promethist.core.Input
import com.promethist.core.Pipeline
import com.promethist.core.model.metrics.Metrics
import com.promethist.core.provider.LocalFileStorage
import com.promethist.core.resources.FileResource
import com.promethist.core.type.Dynamic
import com.promethist.util.LoggerDelegate
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*

object DialogueClient {

    class SimplePipeline(override val components: LinkedList<Component>) : Pipeline {
        override fun process(context: Context): Context {
            var processedContext = context
            if (components.isNotEmpty())
                processedContext = components.pop().process(context)
            return processedContext
        }
    }
    private val logger by LoggerDelegate()

    @JvmStatic
    fun main(args: Array<String>) {

        println("starting...")
        //val fileResource = RestClient.instance(FileResource::class.java, "https://filestore.develop.promethist.com")
        val fileResource = LocalFileStorage(File("test"))
        val loader = FileResourceLoader(fileResource, "dialogue")
        val dm = DialogueManager(loader)
        val ir = object : Component {
            override fun process(context: Context): Context {
                context.turn.input.classes.add(Input.Class(Input.Class.Type.Intent, context.input.transcript.text))
                return context
            }
        }

        val user = User(username = "tester@promethist.ai", name = "Tester", surname = "Tester", nickname = "Tester")
        val profile = Profile(user_id = user._id,  name = user.username)
        val app = Application(name = "test", dialogueName = "product/some-dialogue/1", ttsVoice = "Grace",
                properties = mutableMapOf("some_string" to "bla", "math_max" to 5, "do_math" to true))
        val session = Session(sessionId = "T-E-S-T", user = user, application = app)
        val language = Locale.ENGLISH
        val turn = Turn(Input(language, Input.Transcript("")))
        val context = Context(SimplePipeline(LinkedList(listOf(dm, ir))), profile, session, turn, Metrics(listOf()), logger)

        val reader = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            context.pipeline.process(context)
            logger.info(context.toString())
            println("> ${context.turn.responseItems}")
            if (context.sessionEnded)
                break
            val text = reader.readLine()!!.trim()
            if (text == "exit")
                break
            turn.input = Input(language, Input.Transcript(text))
            turn.attributes.clear()
            turn.responseItems.clear()
            context.pipeline = SimplePipeline(LinkedList(listOf(dm, ir)))
        }
    }
}