package com.promethist.common

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.promethist.common.ServerConfigProvider.ServerConfig
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer
import org.slf4j.LoggerFactory
import javax.servlet.Servlet

class JettyServer(resourceConfig: ResourceConfig, servlets: Map<Class<out Servlet>, String> = emptyMap()) {

    constructor(config: ServerConfig) : this(config.resourceConfig, config.servlets)

    init {
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.toLevel(System.getProperty("com.promethist.common.server.logLevel", "INFO"))
        val server = Server(AppConfig.instance.get("server.port", System.getProperty("com.promethist.common.server.port", "8080")).toInt())
        val servlet = ServletHolder(ServletContainer(resourceConfig))
        val context = ServletContextHandler(server, "/")
        context.addServlet(servlet, "/*")
        servlets.forEach { (servletClass, pathSpec) -> context.addServlet(servletClass, pathSpec) }
        server.start()
        server.join()
        Runtime.getRuntime().addShutdownHook(Thread {
            server.destroy()
        })
    }

    companion object {
        fun run(app: ServerConfigProvider): Unit {
            JettyServer(app.serverConfig)
        }
    }
}