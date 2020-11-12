package com.promethist.port

import com.promethist.common.AppConfig
import com.promethist.common.ServiceUrlResolver
import com.promethist.common.JerseyApplication
import com.promethist.common.ResourceBinder
import com.promethist.core.resources.CoreResource
import com.promethist.core.resources.FileResource
import com.promethist.port.resources.PortResource
import com.promethist.port.resources.PortResourceImpl
import io.sentry.Sentry
import io.sentry.SentryEvent
import java.io.Serializable

class Application : JerseyApplication() {

    init {
        Sentry.init()
        register(object : ResourceBinder() {
            override fun configure() {
                bindTo(AppConfig::class.java, AppConfig.instance)
                bindTo(PortResource::class.java, PortResourceImpl::class.java)
                bindTo(FileResource::class.java, ServiceUrlResolver.getEndpointUrl("filestore"))
                //bindTo(MongoDatabase::class.java,
                //        KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                //                .getDatabase(AppConfig.instance["name"] + "-" + "-" + AppConfig.instance.get("dsuffix", AppConfig.instance["namespace"])))
                bindTo(FileService::class.java)
                bindTo(CoreResource::class.java, ServiceUrlResolver.getEndpointUrl("core"))
            }
        })
    }

    companion object {
        fun capture(e: Throwable, extras: Map<String, Serializable?> = emptyMap()) = with (SentryEvent()) {
            throwable = e
            if (extras.isNotEmpty())
                setExtras(extras)
            Sentry.captureEvent(this)
        }
    }
}