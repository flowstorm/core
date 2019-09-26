package com.promethistai.port

import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.GridFSBuckets
import com.promethistai.common.AppConfig
import com.promethistai.port.model.Contract
import com.promethistai.port.model.Message
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsServiceFactory
import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.findOneById
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*
import javax.activation.MimetypesFileTypeMap
import javax.inject.Inject
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class DataService {

    inner class ResourceFile(val objectId: ObjectId?, val type: String, val name: String?) {

        val bucket = GridFSBuckets.create(database)

        fun download(output: OutputStream) =
            bucket.downloadToStream(objectId, output)


        fun upload(input: InputStream) =
            bucket.uploadFromStream(name!!, input)
    }

    data class CacheItem(val _id: String, var fileId: ObjectId, var lastModified: Date = Date(), var fileSize: Int? = null, var counter: Long = 0, var type: String = "default", var ttsRequest: TtsRequest? = null)

    data class TtsAudio(val speechProvider: String, val ttsRequest: TtsRequest, var cacheItem: CacheItem? = null) {

        // zamerne v code zatim zanedbavam speech providera
        val code = ttsRequest.code()
        var data: ByteArray? = null

        /**
         * Returns or generates audio data if not already set.
         */
        fun data(): ByteArray {
            if (data == null)
                data = TtsServiceFactory.create(speechProvider).use { it.speak(ttsRequest) }
            return data!!
        }
    }

    @Inject
    lateinit var database: MongoDatabase

    @Inject
    lateinit var appConfig: AppConfig

    private var logger = LoggerFactory.getLogger(DataService::class.java)

    private var mediaTypeMap = MimetypesFileTypeMap()

    @Throws(WebApplicationException::class)
    fun getContract(appKey: String): Contract {
        logger.debug("getContract(appKey = $appKey)")

        val col = database.getCollection("contract", Contract::class.java)
        val contract = col.findOne { Contract::appKey eq appKey }

        return if (contract == null)
            throw WebApplicationException("Port contract not found for app key $appKey", Response.Status.NOT_FOUND)
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

    fun addResourceFile(type: String, name: String, input: InputStream) =
        ResourceFile(null, type, name).upload(input)

    fun getCacheItem(id: String): CacheItem? {
        try {
            return database.getCollection("cache", CacheItem::class.java).findOneById(id)
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
    }

    fun saveCacheItem(item: CacheItem) =
            database.getCollection("cache", CacheItem::class.java).save(
                item.apply {
                    counter++
                    lastModified = Date()
                }
            )

    /**
     * This creates and stores or loads existing audio from database for the specified TTS request.
     */
    @Throws(IOException::class)
    internal fun getTtsAudio(speechProvider: String, ttsRequest: TtsRequest): TtsAudio {
        val audio = TtsAudio(speechProvider, ttsRequest)
        var cacheItem = getCacheItem(audio.code)
        if (cacheItem == null) {
            logger.info("getTtsAudio cache MISS ttsRequest = $ttsRequest")
            val fileId = addResourceFile("audio/mp3", ".cache/tts/${audio.code}.mp3", ByteArrayInputStream(audio.data()))
            cacheItem = CacheItem(audio.code, fileId, fileSize = audio.data().size, type = "tts", ttsRequest = ttsRequest)
        } else {
            logger.info("getTtsAudio cache HIT cacheItem = $cacheItem")
            val buf = ByteArrayOutputStream()
            getResourceFile(cacheItem.fileId).download(buf)
            audio.data = buf.toByteArray()
        }
        saveCacheItem(cacheItem)
        audio.cacheItem = cacheItem
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
        val query = org.litote.kmongo.and(Message::appKey eq appKey, Message::recipient eq recipient)
        val messages = col.find(query).toList()
        logger.debug("popMessages(appKey = $appKey, limit = $limit, messages = $messages)")
        col.deleteMany(query)
        return messages
    }

    fun logMessage(message: Message): Boolean {
        val col = database.getCollection("message-log", Message::class.java)
        col.insertOne(message)
        return true
    }
}