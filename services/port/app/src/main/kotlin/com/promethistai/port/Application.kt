package com.promethistai.port

import com.promethistai.common.AppConfig
import com.promethistai.common.JerseyApplication
import com.promethistai.common.ResourceBinder
import com.promethistai.datastore.resources.ObjectResource
import com.promethistai.port.bot.BotService
import com.promethistai.port.bot.IllusionistService
import com.promethistai.port.resources.PortResourceImpl

class Application : JerseyApplication() {

    init {
        register(object : ResourceBinder() {
            override fun configure() {
                bindTo(AppConfig::class.java, AppConfig.instance)
                bindTo(PortResource::class.java, PortResourceImpl::class.java)
                bindTo(BotService::class.java, IllusionistService::class.java)
                bindTo(ObjectResource::class.java, "https://datastore.develop.promethist.ai/")
            }
        })
    }

}