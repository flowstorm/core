package com.promethistai.datastore.model

import com.promethistai.datastore.server.Config
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("check")
@Produces(MediaType.APPLICATION_JSON)
class CheckResource {

    @GET
    fun getCheck(): Check {
        val config = Config.instance
        return Check(
                1.0,
                config["name"],
                config["namespace"],
                config["git.ref"],
                config["git.commit"],
                config["app.image"])
    }
}