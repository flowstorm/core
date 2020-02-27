package com.promethistai.core

import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.promethistai.common.AppConfig
import com.promethistai.common.JerseyApplication
import com.promethistai.common.ResourceBinder
import com.promethistai.core.resources.SessionResource
import com.promethistai.core.resources.SessionResourceImpl
import org.litote.kmongo.KMongo

class Application : JerseyApplication() {

    init {
        register(object : ResourceBinder() {
            override fun configure() {
                bindTo(SessionResource::class.java, SessionResourceImpl::class.java)
                bindTo(MongoDatabase::class.java,
                        KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                                .getDatabase(AppConfig.instance["database.name"]))
            }
        })
    }
}