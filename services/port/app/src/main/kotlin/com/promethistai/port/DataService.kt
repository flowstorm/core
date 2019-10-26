package com.promethistai.port

import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.GridFSBuckets
import com.promethistai.common.AppConfig
import com.promethistai.port.model.Contract
import com.promethistai.port.model.Message
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsServiceFactory
import org.bson.types.ObjectId
import org.litote.kmongo.and
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
import kotlin.concurrent.thread

class DataService {

    inner class ResourceFile(val objectId: ObjectId?, val type: String, val name: String?) {

        val bucket = GridFSBuckets.create(database)

        fun download(output: OutputStream) =
            bucket.downloadToStream(objectId, output)


        fun upload(input: InputStream) =
            bucket.uploadFromStream(name!!, input)
    }

    data class CacheItem(val _id: String, var fileId: ObjectId, var lastModified: Date = Date(), var fileSize: Int? = null, var counter: Long = 0, var type: String = "default", var ttsRequest: TtsRequest? = null)

    inner class TtsAudio(val speechProvider: String, val ttsRequest: TtsRequest) {

        // zamerne v code zatim zanedbavam speech providera
        val code = ttsRequest.code()
        var type = "audio/mp3"
        var data: ByteArray? = null
        var fileId: ObjectId? = null

        /**
         * Returns or generates audio data if not already set.
         */
        fun speak(): TtsAudio {
            if (data == null)
                data = TtsServiceFactory.create(speechProvider).use { it.speak(ttsRequest) }
            return this
        }
    }

    @Inject
    lateinit var database: MongoDatabase

    @Inject
    lateinit var appConfig: AppConfig

    private var logger = LoggerFactory.getLogger(DataService::class.qualifiedName)

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
     * Saves file to database cache (e.g. STT audio) for future usage.
     */
    fun addCacheItemWithFile(id: String, itemType: String, fileType: String, data: ByteArray, ttsRequest: TtsRequest? = null): CacheItem {
        logger.info("addFileToCache(id = $id, itemType = $itemType, fileType = $fileType, data[${data.size}])")
        val fileId = addResourceFile(fileType, ".cache/${itemType}/${id}.mp3", ByteArrayInputStream(data)) //TODO fileType > .ext
        val cacheItem = CacheItem(id, fileId, fileSize = data.size, type = itemType, ttsRequest = ttsRequest)
        saveCacheItem(cacheItem)
        return cacheItem
    }

    /**
     * This creates and stores or loads existing audio from database cache for the specified TTS request.
     */
    @Throws(IOException::class)
    internal fun getTtsAudio(speechProvider: String, ttsRequest: TtsRequest, asyncSave: Boolean, cacheDownload: Boolean): TtsAudio {
        val audio = TtsAudio(speechProvider, ttsRequest)
        var cacheItem = getCacheItem(audio.code)
        if (cacheItem == null) {
            logger.info("getTtsAudio[cache MISS](speechProvider = $speechProvider, ttsRequest = $ttsRequest)")
            audio.speak() // perform speach synthesis
            logger.info("getTtsAudio[speak DONE]")
            if (asyncSave) {
                thread(start = true) {
                    addCacheItemWithFile(audio.code, "tts", audio.type, audio.data!!, ttsRequest)
                }
            } else {
                cacheItem = addCacheItemWithFile(audio.code, "tts", audio.type, audio.data!!, ttsRequest)
                audio.fileId = cacheItem.fileId
            }
        } else {
            logger.info("getTtsAudio[cache HIT](cacheItem = $cacheItem)")
            saveCacheItem(cacheItem) // update cache item in database
            if (cacheDownload) {
                val buf = ByteArrayOutputStream()
                getResourceFile(cacheItem.fileId).download(buf)
                audio.data = buf.toByteArray()
            }
            audio.fileId = cacheItem.fileId
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

    fun logMessage(message: Message): Boolean {
        val col = database.getCollection("message-log", Message::class.java)
        col.insertOne(message)
        return true
    }

    fun getSessionMessages(sessionId: String): List<Message> {
        val logCollection = database.getCollection("message-log", Message::class.java)
        val cacheCollection = database.getCollection("cache", CacheItem::class.java)
        val messages = logCollection.find(Message::sessionId eq sessionId).toList()
        // try to find audio resource files
        for (message in messages) {
            val sttQuery = and(CacheItem::_id eq message._id, CacheItem::type eq "stt")
            val sttItem = cacheCollection.findOne { sttQuery }
        }
        return messages
    }
}