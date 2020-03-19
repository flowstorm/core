package com.promethist.core.nlp.dialogueManager

import com.promethist.core.Context
import com.promethist.core.nlp.NlpAdapter
import com.promethist.core.runtime.DialogueManager
import org.slf4j.LoggerFactory
import javax.inject.Inject

class DialogueManagerNlpAdapter : NlpAdapter {

    @Inject
    lateinit var dialogueManager: DialogueManager

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun process(context: Context): Context {
        logger.info("Dialogue manager NLP adapter called")

        val ended = if (context.turn.input.text == "\$intro") {
            //TODO we need dialogue parameters from application
//            dialogueManager.start(context.session.application.dialogueName, context, arrayOf())
            dialogueManager.start("product/some-dialogue/1/model", context, arrayOf("ble", 5, true))
        } else {
            dialogueManager.proceed(context)
        }

        logger.info(context.toString())

        return context
    }
}