package com.promethistai.port

import com.mongodb.ConnectionString
import com.promethistai.common.AppConfig
import org.litote.kmongo.KMongo

object Test {

    @JvmStatic
    fun main(args: Array<String>) {
        val ds = DataService()
        ds.appConfig = AppConfig.instance
        ds.database = KMongo.createClient(ConnectionString(ds.appConfig["database.url"])).getDatabase(ds.appConfig["database.name"])

       val messages = ds.getSessionMessages("849a45cc-02e1-4bac-ba78-b43cf79bf202")
        for (message in messages) {
            println(message)
        }
    }
}