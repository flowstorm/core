package com.promethistai.port.resources

import com.promethistai.port.ConfigService
import com.promethistai.port.PortConfig
import com.promethistai.port.bot.BotService
import com.promethistai.port.bot.Message
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsServiceFactory
import com.promethistai.port.tts.TtsVoice
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.ws.rs.*

@Path("/")
class PortResourceImpl : PortResource {

    private var logger = LoggerFactory.getLogger(PortResourceImpl::class.java)

    /**
     * Example of dependency injection
     * @see com.promethistai.port.Application constructor
     */
    @Inject
    lateinit var botService: BotService

    @Inject
    lateinit var configService: ConfigService

    override fun getConfig(key: String): PortConfig = configService.getConfig(key)

    override fun message(key: String, message: Message): Message? {
        return botService.message(key, message)
    }

    override fun messageQueuePush(key: String, message: Message): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        if (logger.isInfoEnabled)
            logger.info("key = $key, message = $message")
    }

    override fun messageQueuePop(key: String, limit: Int): List<Message> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        val messages = mutableListOf<Message>()
        if (logger.isInfoEnabled)
            logger.info("key = $key, limit = $limit, response = $messages")
    }

    override fun tts(provider: String, request: TtsRequest): ByteArray {
        if (logger.isInfoEnabled)
            logger.info("provider = $provider, request = $request")
        return TtsServiceFactory.create(provider).speak(request.text!!, request.voice!!, request.language!!)
    }

    override fun ttsVoices(provider: String): List<TtsVoice> {
        if (logger.isInfoEnabled)
            logger.info("provider = $provider")
        return TtsServiceFactory.create(provider).voices
    }

    override fun ttsBR(request: TtsRequest): ByteArray {
        return tts("google", request)
    }

    override fun ttsVoicesBR(): List<TtsVoice> {
        return ttsVoices("google")
    }
}