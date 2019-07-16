package com.promethistai.datastore.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.promethistai.datastore.model.CheckResource
import com.promethistai.datastore.model.KindResource
import com.promethistai.datastore.model.ObjectResource
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider
import org.glassfish.jersey.server.ResourceConfig
import java.net.URI
import javax.ws.rs.core.Application
import javax.ws.rs.ext.ContextResolver

object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        val resourceConfig = ResourceConfig.forApplication(object : Application() {
            override fun getSingletons(): MutableSet<Any> {
                return mutableSetOf(ObjectResource(), KindResource(), CheckResource())
            }
        }).register(ContextResolver<ObjectMapper> {
            ObjectMapper().registerModule(KotlinModule())
        })
        val server = NettyHttpContainerProvider.createHttp2Server(URI.create("http://localhost:8080/"), resourceConfig, null)
        Runtime.getRuntime().addShutdownHook(Thread(Runnable { server.close() }))
    }
}
