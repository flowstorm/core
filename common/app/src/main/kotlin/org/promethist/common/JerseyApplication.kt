package org.promethist.common

import org.glassfish.jersey.internal.inject.InjectionManager
import org.glassfish.jersey.logging.LoggingFeature
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.server.spi.Container
import org.glassfish.jersey.server.spi.ContainerLifecycleListener
import org.promethist.common.ServerConfigProvider.ServerConfig
import org.promethist.util.LoggerDelegate
import java.util.logging.Level
import java.util.logging.Logger
import javax.ws.rs.ext.ContextResolver
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

open class JerseyApplication : ResourceConfig(), ServerConfigProvider {

    lateinit var injectionManager: InjectionManager
    protected val logger by LoggerDelegate()

    init {
        AppConfig.instance.let {
            logger.info("Starting application (version=${it["git.ref"]}, namespace=${it["namespace"]})")
        }
        registerDefaultPackages()

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

    operator fun <T> get(type: Class<T>): T {
        return injectionManager.getInstance(type)
    }

    //Make register final to avoid calling non final method in constructor
    final override fun register(component: Any?): ResourceConfig = super.register(component)

    /**
     * Automatically register default packages from all child classes
     */
    private fun registerDefaultPackages() {
        var superClasses = this::class.superclasses
        val classes = mutableListOf<KClass<*>>(this::class)

        while (superClasses.isNotEmpty()) {
            superClasses = superClasses.firstOrNull { !it::class.java.isInterface }?.let {
                classes.add(it)
                if (it != JerseyApplication::class) {
                    it.superclasses
                } else null
            } ?: listOf()
        }

        classes.map { it.java.`package`.name }.distinct().forEach {
            packages("$it.filters", "$it.resources")
        }
    }

    override val serverConfig get() = ServerConfig(this)

    companion object {

        lateinit var instance: JerseyApplication

        @JvmStatic
        fun main(args: Array<String>) = JettyServer.run(JerseyApplication())
    }
}