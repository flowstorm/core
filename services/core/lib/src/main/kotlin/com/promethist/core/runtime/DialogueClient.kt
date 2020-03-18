package com.promethist.core.runtime

import com.promethist.core.Context
import com.promethist.core.model.*
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
        val profile = Profile(name = user.username)
        val application = Application(name = "test", dialogueName = "product/some-dialogue/1", ttsVoice = "Grace")
        val session = Session(sessionId = "T-E-S-T", user = user, application = application)
        val turn = Turn("\$intro")
        val context = Context(profile, session, turn, logger)
// start or proceed
        dm.start("${session.application.dialogueName}/model", context, arrayOf("ble", 5, true))
        println(context)

        val reader = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            turn.input = reader.readLine()!!.trim()
            if (turn.input == "exit")
                break
            turn.attributes.clear()
            turn.responseItems.clear()
            val proceed = dm.proceed(context)
            logger.info(context.toString())
            println("> ${context.turn.responseItems}")
            if (!proceed)
                break
        }
    }
}