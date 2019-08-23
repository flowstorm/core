package com.promethistai.port

import com.mongodb.client.MongoDatabase
import com.promethistai.common.AppConfig
import com.promethistai.port.model.Contract
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

class ConfigService {

    @Inject
    lateinit var database: MongoDatabase

    @Inject
    lateinit var appConfig: AppConfig

    private var logger = LoggerFactory.getLogger(ConfigService::class.java)

    @Throws(WebApplicationException::class)
    fun getConfig(key: String): PortConfig {
        if (logger.isDebugEnabled)
            logger.debug("getConfig key=$key")

        val col = database.getCollection("contract", Contract::class.java)
        val contract = col.findOne { Contract::key eq key }

        return if (contract == null)
            throw WebApplicationException("Port contract not found for key $key", Response.Status.NOT_FOUND)
        else
            PortConfig(appConfig["service.host"], contract)
    }
}