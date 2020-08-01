package com.promethist.port.stt

object SttServiceFactory {

    fun create(provider: String, callback: SttCallback): SttService {
        when (provider) {
            "Google" -> return GoogleSttService(callback)
            else -> throw NotImplementedError()
        }
    }
}