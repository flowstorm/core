package ai.flowstorm.core.stt

import ai.flowstorm.core.ExpectedPhrase
import ai.flowstorm.core.model.SttConfig

interface SttService: AutoCloseable {

    fun createStream(config: SttConfig, expectedPhrases: List<ExpectedPhrase>): SttStream

}