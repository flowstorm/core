package com.promethist.port.stt

import com.promethist.core.ExpectedPhrase

object SttServiceFactory {

    fun create(provider: String, config: SttConfig, expectedPhrases: List<ExpectedPhrase>, callback: SttCallback): SttService {
        when (provider) {
            "Google" -> return GoogleSttService(config, callback, expectedPhrases)
            else -> throw NotImplementedError()
        }
    }
}