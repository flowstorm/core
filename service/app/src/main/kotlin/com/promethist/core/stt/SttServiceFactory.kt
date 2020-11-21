package com.promethist.core.stt

object SttServiceFactory {

    fun create(provider: String, callback: SttCallback): SttService {
        when (provider) {
            "Google" -> return GoogleSttService(callback)
            else -> throw NotImplementedError()
        }
    }
}