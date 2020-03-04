package com.promethist.core

import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.promethist.common.AppConfig
import com.promethist.common.JerseyApplication
import com.promethist.common.ResourceBinder
import com.promethist.core.resources.BotService
import com.promethist.core.resources.SessionResource
import com.promethist.core.resources.SessionResourceImpl
import org.litote.kmongo.KMongo

class Application : JerseyApplication() {

    init {
        register(object : ResourceBinder() {
            override fun configure() {
                bindTo(SessionResource::class.java, SessionResourceImpl::class.java)
                bindTo(MongoDatabase::class.java,
                        KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                                .getDatabase(AppConfig.instance["database.name"]))

                // manager
                val dialogueManagerUrl =
                        AppConfig.instance.get("dialoguemanager.url",
                                ServiceUtil.getEndpointUrl("helena",
                                        ServiceUtil.RunMode.valueOf(AppConfig.instance.get("runmode", "dist")))) + "/dm"
                println("dialogueManagerUrl = $dialogueManagerUrl")
                bindTo(BotService::class.java, dialogueManagerUrl)


            }
        })
    }
}