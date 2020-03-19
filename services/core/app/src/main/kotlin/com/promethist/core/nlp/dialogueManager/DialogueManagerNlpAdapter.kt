package com.promethist.core.nlp.dialogueManager

import com.promethist.core.Context
import com.promethist.core.model.MessageItem
import com.promethist.core.nlp.NlpAdapter
import org.slf4j.LoggerFactory

class DialogueManagerNlpAdapter : NlpAdapter {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun process(context: Context): Context {
        logger.info("Dialogue manager NLP adapter called")

        logger.info(context.toString())
        context.turn.responseItems.add(MessageItem("Hi there"))

        return context
    }
}