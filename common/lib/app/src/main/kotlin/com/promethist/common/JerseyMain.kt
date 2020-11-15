package com.promethist.common

import org.glassfish.jersey.server.ResourceConfig

object JerseyMain {

    @JvmStatic
    fun main(args: Array<String>) {
        val appClassName = if (args.isEmpty() || "default" == args[0]) JerseyApplication::class.java.name else args[0]
        val serverType = if (args.size > 1) args[1] else "jetty"
        val resourceConfig = Class.forName(appClassName).newInstance() as ResourceConfig
        when (serverType) {
            "netty" -> NettyServer(resourceConfig)
            else -> JettyServer(resourceConfig)
        }
    }
}