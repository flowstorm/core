package com.promethistai.port.resources

import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.GridFSBuckets
import com.promethistai.port.ConfigService
import com.promethistai.port.PortConfig
import com.promethistai.port.bot.BotService
import com.promethistai.port.bot.Message
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsServiceFactory
import com.promethistai.port.tts.TtsVoice
import org.bson.types.ObjectId
import org.litote.kmongo.findOneById
import org.slf4j.LoggerFactory
import java.net.URLConnection
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

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

    @Inject
    lateinit var database: MongoDatabase

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

    override fun readFile(id: String): Response {
        val responseBuilder: Response.ResponseBuilder
        val objectId = ObjectId(id)
        val file = database.getCollection("fs.files").findOneById(objectId)
        if (file != null) {
            val filename = file["filename"]
            val mimeType =
                if (filename == null)
                    MediaType.APPLICATION_OCTET_STREAM
                else
                    URLConnection.guessContentTypeFromName(filename as String)

            val bucket = GridFSBuckets.create(database)
            responseBuilder =
                Response.ok(
                    StreamingOutput { output ->
                        try {
                            bucket.downloadToStream(objectId, output)
                            output.flush()
                        } catch (e: Exception) {
                            throw WebApplicationException("File streaming failed", e)
                        }
                    }, mimeType).header("Content-Disposition", "inline" + if (filename == null) "" else "; filename=\"$filename\"")
        } else {
            responseBuilder = Response.status(Response.Status.NOT_FOUND)
        }
        return responseBuilder.build()
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