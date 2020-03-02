package com.promethistai.port

import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.promethistai.common.AppConfig
import com.promethistai.common.JerseyApplication
import com.promethistai.common.ResourceBinder
import com.promethistai.core.ServiceUtil
import com.promethistai.core.resources.BotService
import com.promethistai.filestore.resources.FileResource
import com.promethistai.port.bot.*
import com.promethistai.port.resources.PortResource
import com.promethistai.port.resources.PortResourceImpl
import org.litote.kmongo.KMongo

class Application : JerseyApplication() {

    companion object {
        val filestoreUrl = ServiceUtil.getEndpointUrl("filestore",
                ServiceUtil.RunMode.valueOf(AppConfig.instance["runmode"]))
    }

    init {
        register(object : ResourceBinder() {
            override fun configure() {
                bindTo(AppConfig::class.java, AppConfig.instance)
                bindTo(PortResource::class.java, PortResourceImpl::class.java)
                bindTo(FileResource::class.java, filestoreUrl)
                bindTo(MongoDatabase::class.java, KMongo
                        .createClient(ConnectionString(AppConfig.instance["database.url"]))
                        .getDatabase(AppConfig.instance["database.name"]))
                // services
                bindTo(DataService::class.java)
                bindTo(BotService::class.java, BotSelectorService::class.java)
                bindTo(BotRemoteService::class.java)
                bindTo(EchoService::class.java)
                bindTo(IllusionistService::class.java)
            }
        })
    }
}