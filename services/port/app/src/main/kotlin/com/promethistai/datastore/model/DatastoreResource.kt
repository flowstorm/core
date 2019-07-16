package com.promethistai.datastore.model

import com.google.cloud.Timestamp
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.KeyFactory
import com.promethistai.datastore.server.Config
import java.util.*

open class DatastoreResource {

    protected val datastore = DatastoreOptions.getDefaultInstance().service
    protected val namespace = Config.instance["namespace"]

    private val keyFactories: MutableMap<String, KeyFactory> = Hashtable()

    protected fun getKeyFactory(component: String, type: String): KeyFactory {
        val factoryId = "$namespace/$component/$type"
        var keyFactory = keyFactories[factoryId]
        if (keyFactory == null) {
            keyFactory = datastore.newKeyFactory().setNamespace("$namespace/$component").setKind(type)
            keyFactories[factoryId] = keyFactory
        }
        return keyFactory!!
    }

    fun createEntity(component: String, type: String, apiKey: String, scope: String): Entity {
        val key = datastore.allocateId(getKeyFactory(component, type).newKey())
        val entityBuilder = Entity.newBuilder(key)
                //.set("_app", app)
                .set("_key", apiKey)
                .set("_scope", scope)

                //.set("description", StringValue.newBuilder("test....").setExcludeFromIndexes(true).build())
                .set("_created", Timestamp.now())

        return entityBuilder.build()
    }

    fun deleteEntity(component: String, type: String, id: Long) {
        datastore.delete(getKeyFactory(component, type).newKey(id))
    }
}