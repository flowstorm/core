package com.promethistai.common

import org.glassfish.jersey.logging.LoggingFeature
import org.glassfish.jersey.server.ResourceConfig
import java.util.logging.Level
import java.util.logging.Logger
import javax.ws.rs.ApplicationPath

@ApplicationPath("/")
open class JerseyApplication : ResourceConfig() {

    init {
        packages(
            "com.promethistai.common.filters",
            "com.promethistai.common.resources",
            AppConfig.instance["package"] + ".filters",
            AppConfig.instance["package"] + ".resources"
        )
        if ("TRUE" == AppConfig.instance["app.logging"])
            register(LoggingFeature(Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME), Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, 10000))
    }

    companion object {
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
}