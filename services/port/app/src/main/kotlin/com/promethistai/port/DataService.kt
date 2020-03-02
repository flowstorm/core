package com.promethistai.port

import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.GridFSBuckets
import com.promethistai.common.AppConfig
import com.promethistai.core.model.Message
import com.promethistai.port.model.Contract
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsServiceFactory
import com.promethistai.filestore.resources.FileResource
import org.bson.types.ObjectId
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.findOneById
import org.slf4j.LoggerFactory
import java.io.*
import javax.activation.MimetypesFileTypeMap
import javax.inject.Inject
import javax.ws.rs.NotFoundException
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.concurrent.thread

class DataService {

    @Inject
    lateinit var config: AppConfig

    inner class ResourceFile(val objectId: ObjectId?, val type: String, val name: String?) {

        val bucket = GridFSBuckets.create(database)

        fun download(output: OutputStream) = bucket.downloadToStream(objectId, output)

        //fun upload(input: InputStream) = bucket.uploadFromStream(name!!, input)
    }

    inner class TtsAudio(val ttsRequest: TtsRequest) {

        val code = ttsRequest.code()
        var type = "audio/mpeg"
        var data: ByteArray? = null
        var path: String? = null

        /**
         * Returns or generates audio data if not already set.
         */
        fun speak(): TtsAudio {
            if (data == null) {
                try {
                    data = TtsServiceFactory.speak(ttsRequest)
                } catch (e: Throwable) {
                    throw IOException(e.message, e)
                }
            }
            return this
        }
    }

    @Inject
    lateinit var database: MongoDatabase

    @Inject
    lateinit var filestore: FileResource

    @Inject
    lateinit var appConfig: AppConfig

    private var logger = LoggerFactory.getLogger(DataService::class.qualifiedName)

    private var mediaTypeMap = MimetypesFileTypeMap()

    @Throws(WebApplicationException::class)
    fun getContract(appKey: String): Contract {
        logger.debug("getContract(appKey = $appKey)")

        val col = database.getCollection("contract", Contract::class.java)
        val contract = col.findOne {
            Contract::appKey eq
                if (appKey.contains(':')) appKey.substring(0, appKey.indexOf(':')) else appKey
        }

        return if (contract == null)
            throw NotFoundException("Port contract not found for app key $appKey")
        else
            contract
    }

    fun getResourceFile(objectId: ObjectId): ResourceFile {
        val fileDocument = database.getCollection("fs.files").findOneById(objectId)
        if (fileDocument != null) {
            val name = fileDocument["filename"]?.toString()
            var type =
                if (name != null)
                    mediaTypeMap.getContentType(name)
                else
                    MediaType.APPLICATION_OCTET_STREAM
            return ResourceFile(objectId, type, name)
        } else {
            throw WebApplicationException("Resource file not found for object id $objectId", Response.Status.NOT_FOUND)
        }
    }

    //fun addResourceFile(type: String, name: String, input: InputStream) =
    //    ResourceFile(null, type, name).upload(input)

    /**
     * Saves TTS audio to filestorefor future usage.
     */
    fun saveTtsAudio(code: String, type: String, data: ByteArray, ttsRequest: TtsRequest) {
        logger.info("saveTtsAudio(code = $code, fileType = $type, data[${data.size}])")
        filestore.writeFile("tts/${ttsRequest.voice}/$code", type, listOf("text:${ttsRequest.text}"), data.inputStream())
    }

    /**
     * This creates and stores or loads existing audio from database cache for the specified TTS request.
     */
    @Throws(IOException::class)
    internal fun getTtsAudio(ttsRequest: TtsRequest, asyncSave: Boolean, download: Boolean): TtsAudio {
        val audio = TtsAudio(ttsRequest)
        val path = "tts/${ttsRequest.voice}/${audio.code}"
        try {
            val ttsFile = filestore.getFile(path)
            logger.info("getTtsAudio[HIT](ttsRequest = $ttsRequest)")
            if (download)
                audio.data = filestore.readFile(path).readEntity(ByteArray::class.java)
            audio.path = path
        } catch (e: NotFoundException) {
            logger.info("getTtsAudio[MISS](ttsRequest = $ttsRequest)")
            audio.speak() // perform speach synthesis
            logger.info("getTtsAudio[DONE]")
            if (asyncSave) {
                thread(start = true) {
                    saveTtsAudio(audio.code,  audio.type, audio.data!!, ttsRequest)
                }
            } else {
                saveTtsAudio(audio.code, audio.type, audio.data!!, ttsRequest)
                audio.path = path
            }
        }
        return audio
    }

    fun pushMessage(appKey: String, message: Message): Boolean {
        logger.info("pushMessage(appKey = $appKey, message = $message)")

        if (message._id == null)
            message._id = ObjectId.get().toHexString()
        message.appKey = appKey

        val col = database.getCollection("message-queue", Message::class.java)
        col.insertOne(message)
        return true
    }

    fun popMessages(appKey: String, recipient: String, limit: Int): List<Message> {
        val col = database.getCollection("message-queue", Message::class.java)
        val query = and(Message::appKey eq appKey, Message::recipient eq recipient)
        val messages = col.find(query).toList()
        logger.debug("popMessages(appKey = $appKey, limit = $limit, messages = $messages)")
        col.deleteMany(query)
        return messages
    }
}