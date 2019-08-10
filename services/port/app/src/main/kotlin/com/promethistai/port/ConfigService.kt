package com.promethistai.port

import com.promethistai.common.AppConfig
import com.promethistai.datastore.resources.Object
import com.promethistai.datastore.resources.ObjectResource
import com.promethistai.port.resources.PortConfig
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
                Object().set("key", key))
        */
        val contracts = objectResource.filterObjects("port", "contract", appConfig["apiKey"], Object().set("key", key))

        return if (contracts.isEmpty())
            throw WebApplicationException("Port contract not found for key $key", Response.Status.NOT_FOUND)
        else
            PortConfig(appConfig["service.host"], contracts[0])
    }
}