package com.promethist.common

import org.glassfish.jersey.internal.inject.InjectionManager
import org.glassfish.jersey.logging.LoggingFeature
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.server.spi.Container
import org.glassfish.jersey.server.spi.ContainerLifecycleListener
import java.util.logging.Level
import java.util.logging.Logger
import javax.ws.rs.ext.ContextResolver

open class JerseyApplication : ResourceConfig() {

    lateinit var injectionManager: InjectionManager

    init {
        packages(
                "com.promethist.common.filters",
                "com.promethist.common.resources",
                AppConfig.instance["package"] + ".filters",
                AppConfig.instance["package"] + ".resources"
        )
        register(object : ContainerLifecycleListener {
            override fun onStartup(container: Container) {
                injectionManager = container.applicationHandler.injectionManager
            }

            override fun onReload(container: Container) {
            }

            override fun onShutdown(container: Container) {
            }
        })

        register(ContextResolver { ObjectUtil.defaultMapper })

        if ("TRUE" == AppConfig.instance["app.logging"])
            register(LoggingFeature(Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME), Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, 10000))

        instance = this
    }

    operator fun <T>get(type: Class<T>): T {
        return injectionManager.getInstance(type)
    }

    companion object {

        lateinit var instance: JerseyApplication

        @JvmStatic
        fun main(args: Array<String>) {
            JerseyMain.main(args)
        }
    }
}