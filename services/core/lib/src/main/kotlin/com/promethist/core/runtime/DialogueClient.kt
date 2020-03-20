package com.promethist.core.runtime

import com.promethist.core.nlp.Context
import com.promethist.core.model.*
import com.promethist.core.nlp.Input
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object DialogueClient {

    private val logger = LoggerFactory.getLogger(this::class.qualifiedName)

    @JvmStatic
    fun main(args: Array<String>) {

        println("starting...")
        val dm = DialogueManager(LocalFileLoader(File("test/dialogue")))

        val user = User(username = "tester@promethist.ai", name = "Tester", surname = "Tester", nickname = "Tester")
        val profile = Profile(user_id = user._id,  name = user.username)
        val application = Application(name = "test", dialogueName = "product/some-dialogue/1", ttsVoice = "Grace",
                properties = mutableMapOf("some_string" to "bla", "math_max" to 5, "do_math" to true))
        val session = Session(sessionId = "T-E-S-T", user = user, application = application)
        val turn = Turn(Input(""))
        val context = Context(profile, session, turn, logger)

        val reader = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            dm.proceed(context)
            logger.info(context.toString())
            println("> ${context.turn.responseItems}")
            turn.input.text = reader.readLine()!!.trim()
            if (turn.input.text == "exit")
                break
            turn.attributes.clear()
            turn.responseItems.clear()
        }
    }
}