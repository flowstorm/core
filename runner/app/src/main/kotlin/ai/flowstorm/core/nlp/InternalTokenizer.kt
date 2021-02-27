package ai.flowstorm.core.nlp

import ai.flowstorm.core.Component
import ai.flowstorm.core.Context
import ai.flowstorm.core.Input
import ai.flowstorm.core.dialogue.tokenize
import ai.flowstorm.util.LoggerDelegate

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
            logger.info("Tokens $tokens")
            alternatives.add(0, transcript)
            if (words.size > DEFAULT_MAX_WORDS) {
                transcript = Input.Transcript("#toomanywords")
            }
        }
        return context.pipeline.process(context)
    }
}