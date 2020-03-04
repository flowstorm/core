package com.promethist.port

import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.promethist.common.AppConfig
import com.promethist.common.JerseyApplication
import com.promethist.common.ResourceBinder
import com.promethist.core.ServiceUtil
import com.promethist.core.resources.BotService
import com.promethist.filestore.resources.FileResource
import com.promethist.port.bot.*
import com.promethist.port.resources.PortResource
import com.promethist.port.resources.PortResourceImpl
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