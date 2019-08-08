package com.promethistai.port

import com.promethistai.common.AppConfig
import com.promethistai.common.JerseyApplication
import com.promethistai.common.ResourceBinder
import com.promethistai.datastore.resources.ObjectResource
import com.promethistai.port.bot.*
import com.promethistai.port.resources.PortResourceImpl

class Application : JerseyApplication() {

    init {
        register(object : ResourceBinder() {
            override fun configure() {
                bindTo(AppConfig::class.java, AppConfig.instance)
                // resources
                bindTo(PortResource::class.java, PortResourceImpl::class.java)
                bindTo(ObjectResource::class.java, AppConfig.instance["datastore.endpoint"])
                // services
                bindTo(ConfigService::class.java, ConfigService::class.java)
                bindTo(BotService::class.java, BotSelectorService::class.java)
                bindTo(BotRemoteService::class.java, BotRemoteService::class.java)
                bindTo(EchoService::class.java, EchoService::class.java)
                bindTo(IllusionistService::class.java, IllusionistService::class.java)
            }
        })
    }

}