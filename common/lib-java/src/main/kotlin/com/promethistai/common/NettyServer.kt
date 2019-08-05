package com.promethistai.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.server.ServerProperties
import java.net.URI
import javax.ws.rs.ext.ContextResolver

/**
 * @author Tomas Zajicek <tomas.zajicek@promethist.ai>
 */
class NettyServer(private val resourceConfig: ResourceConfig) {

    private val channel =
        NettyHttpContainerProvider.createHttp2Server(
            URI.create("http://localhost:" + (AppConfig.instance["server.port"]?:"8080") + "/"),
            resourceConfig.register(ContextResolver<ObjectMapper> {
                ObjectMapper().registerModule(KotlinModule())
            }).property(ServerProperties.TRACING, AppConfig.instance["app.tracing"]?:"OFF"),
            null
        )

    init {
        Runtime.getRuntime().addShutdownHook(Thread(Runnable {
            channel.close()
        }))
    }

}
