package com.promethist.core.runtime

import com.promethist.common.RestClient
import com.promethist.core.Context
import com.promethist.core.model.*
import com.promethist.core.Input
import com.promethist.core.model.metrics.Metrics
import com.promethist.core.resources.FileResource
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

object DialogueClient {

    private val logger = LoggerFactory.getLogger(this::class.qualifiedName)

    @JvmStatic
    fun main(args: Array<String>) {

        println("starting...")
        val fileResource = RestClient.instance(FileResource::class.java, "https://filestore.develop.promethist.com")
        //val fileResource = LocalFileStorage(File("test"))
        val loader = FileResourceLoader(fileResource, "dialogue")
        val dm = DialogueManager(loader)

        val user = User(username = "tester@promethist.ai", name = "Tester", surname = "Tester", nickname = "Tester")
        val profile = Profile(user_id = user._id,  name = user.username)
        val app = Application(name = "test", dialogueName = "product/dialogue/1", ttsVoice = "Grace",
                properties = mutableMapOf("some_string" to "bla", "math_max" to 5, "do_math" to true))
        val session = Session(sessionId = "T-E-S-T", user = user, application = app)
        val language = Locale.ENGLISH
        val turn = Turn(Input(language, Input.Transcript("")))
        val context = Context(profile, session, turn, Metrics(listOf()), logger)

        val reader = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            dm.process(context)
            logger.info(context.toString())
            println("> ${context.turn.responseItems}")
            if (context.sessionEnded)
                break
            val text = reader.readLine()!!.trim()
            if (text == "exit")
                break
            turn.input = Input(language, Input.Transcript(text), classes = mutableListOf(Input.Class(Input.Class.Type.Intent, text)))
            turn.attributes.clear()
            turn.responseItems.clear()
        }
    }
}