package com.promethistai.port

import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.GridFSBuckets
import com.promethistai.common.AppConfig
import com.promethistai.port.model.Contract
import com.promethistai.port.model.Message
import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.findOneById
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.net.URLConnection
import javax.inject.Inject
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class DataService {

    inner class ResourceFile(val objectId: ObjectId, val type: String, val name: String?) {

        fun download(output: OutputStream) {
            val bucket = GridFSBuckets.create(database)
            bucket.downloadToStream(objectId, output)
        }
    }

    @Inject
    lateinit var database: MongoDatabase

    @Inject
    lateinit var appConfig: AppConfig

    private var logger = LoggerFactory.getLogger(DataService::class.java)

    @Throws(WebApplicationException::class)
    fun getContract(key: String): Contract {
        if (logger.isDebugEnabled)
            logger.debug("getConfig key=$key")

        val col = database.getCollection("contract", Contract::class.java)
        val contract = col.findOne { Contract::key eq key }

        return if (contract == null)
            throw WebApplicationException("Port contract not found for key $key", Response.Status.NOT_FOUND)
        else
            contract
    }

    fun getResourceFile(id: String): ResourceFile {
        val objectId = ObjectId(id)
        val fileDocument = database.getCollection("fs.files").findOneById(objectId)
        if (fileDocument != null) {
            val filename = fileDocument["filename"]
            return ResourceFile(objectId,
                    if (filename == null)
                        MediaType.APPLICATION_OCTET_STREAM
                    else
                        URLConnection.guessContentTypeFromName(filename as String),
                    if (filename == null)
                        null
                    else
                        filename as String
            )
        } else {
            throw WebApplicationException("Resource file not found for id $id", Response.Status.NOT_FOUND)
        }
    }

    fun pushMessage(key: String, message: Message): Boolean {
        if (logger.isInfoEnabled)
            logger.info("key = $key, message = $message")

        if (message._id == null)
            message._id = ObjectId.get().toHexString()
        message.customer = key

        val col = database.getCollection("message", Message::class.java)
        col.insertOne(message)
        return true
    }

    fun popMessages(key: String, recipient: String, limit: Int): List<Message> {
        val col = database.getCollection("message", Message::class.java)
        val query = org.litote.kmongo.and(Message::customer eq key, Message::recipient eq recipient)
        val messages = col.find(query).toList()
        if (logger.isInfoEnabled)
            logger.info("key = $key, limit = $limit, messages = $messages")

        col.deleteMany(query)
        return messages
    }
}