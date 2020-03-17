package com.promethist.core.runtime

import com.promethist.core.model.Application
import com.promethist.core.model.Context
import com.promethist.core.model.Session
import com.promethist.core.model.User
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object DialogueClient {
    @JvmStatic
    fun main(args: Array<String>) {

        println("starting...")
        val dm = DialogueManager(LocalFileLoader(File("test/dialogue")))

        val user = User(username = "tester@promethist.ai", name = "Tester", surname = "Tester", nickname = "Tester")
        val application = Application(name = "test", dialogueName = "product/some-dialogue/1", ttsVoice = "Grace")
        val session = Session(sessionId = "T-E-S-T", user = user, application = application)
        val context = Context("\$intro")
// start or proceed
        dm.start("${session.application.dialogueName}/model", session, context, arrayOf("ble", 5, true))
        println(context)

        val reader = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            context.input = reader.readLine()!!.trim()
            if (context.input == "exit")
                break
            context.attributes.clear()
            context.responseItems.clear()
            val proceed = dm.proceed(session, context)
            println(context)
            if (!proceed)
                break
        }
    }
}