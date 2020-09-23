package com.promethist.port

import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.promethist.common.AppConfig
import com.promethist.common.ServiceUrlResolver
import com.promethist.common.JerseyApplication
import com.promethist.common.ResourceBinder
import com.promethist.core.resources.CoreResource
import com.promethist.core.resources.FileResource
import com.promethist.port.resources.PortResource
import com.promethist.port.resources.PortResourceImpl
import io.sentry.Sentry
import org.litote.kmongo.KMongo
import javax.ws.rs.NotFoundException

class Application : JerseyApplication() {

    init {
        Sentry.init()
        register(object : ResourceBinder() {
            override fun configure() {
                bindTo(AppConfig::class.java, AppConfig.instance)
                bindTo(PortResource::class.java, PortResourceImpl::class.java)
                bindTo(FileResource::class.java, ServiceUrlResolver.getEndpointUrl("filestore"))
                bindTo(MongoDatabase::class.java,
                        KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                                .getDatabase(AppConfig.instance["name"] + "-" + AppConfig.instance["namespace"]))
                bindTo(PortService::class.java)
                bindTo(CoreResource::class.java, ServiceUrlResolver.getEndpointUrl("core"))
            }
        })
    }
}