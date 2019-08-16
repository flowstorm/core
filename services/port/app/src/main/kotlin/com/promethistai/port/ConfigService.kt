package com.promethistai.port

import com.promethistai.common.AppConfig
import com.promethistai.datastore.DatastoreObject
import com.promethistai.datastore.resources.ObjectResource
import javax.inject.Inject
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

class ConfigService {

    @Inject
    lateinit var objectResource: ObjectResource

    @Inject
    lateinit var appConfig: AppConfig

    fun getConfig(key: String): PortConfig {
        /*
        val contracts = objectResource.queryObjects("port", appConfig["apiKey"],
                "SELECT * FROM contract WHERE key=@key",
                Object(mapOf("key" to key)))
        */
        val contracts = objectResource.filterObjects("port", "contract", appConfig["apiKey"], DatastoreObject(mapOf("key" to key)))

        return if (contracts.isEmpty())
            throw WebApplicationException("Port contract not found for key $key", Response.Status.NOT_FOUND)
        else
            PortConfig(appConfig["service.host"], contracts[0])
    }
}