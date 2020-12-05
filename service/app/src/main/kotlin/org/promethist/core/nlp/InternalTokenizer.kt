package org.promethist.core.nlp

import org.promethist.core.Component
import org.promethist.core.Context
import org.promethist.core.Input
import org.promethist.core.dialogue.tokenize
import org.promethist.util.LoggerDelegate

class InternalTokenizer : Component {

    companion object {
        const val DEFAULT_MAX_WORDS = 12
    }
    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {

        with (context.input) {
            if (tokens.isEmpty()) {
                // tokenization has not been done yet (ASR skipped?)
                tokens.addAll(transcript.text.tokenize())
            }
            logger.info("tokens $tokens")
            alternatives.add(0, transcript)
            if (words.size > DEFAULT_MAX_WORDS) {
                transcript = Input.Transcript("#toomanywords")
            }
        }
        return context.pipeline.process(context)
    }
}