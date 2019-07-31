package com.promethistai.common

import org.glassfish.jersey.logging.LoggingFeature
import org.glassfish.jersey.server.ResourceConfig
import java.util.logging.Level
import java.util.logging.Logger
import javax.ws.rs.ApplicationPath

@ApplicationPath("/")
class JerseyApplication : ResourceConfig() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            NettyServer(JerseyApplication())
        }
    }

    init {
        packages(
            "com.promethistai.common.filters",
            "com.promethistai.common.resources",
            Config.instance["package"] + ".filters",
            Config.instance["package"] + ".resources"
        )
        if ("TRUE" == Config.instance["app.logging"])
            register(LoggingFeature(Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME), Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, 10000))
    }
}