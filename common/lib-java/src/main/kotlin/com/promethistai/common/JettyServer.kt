package com.promethistai.common

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer

class JettyServer(private val resourceConfig: ResourceConfig) {

    private val server = Server(AppConfig.instance.get("server.port", "8080").toInt())

    init {
        val servlet = ServletHolder(ServletContainer(resourceConfig))
        val context = ServletContextHandler(server, "/")
        context.addServlet(servlet, "/*")
        server.start()
        server.join()
        Runtime.getRuntime().addShutdownHook(Thread(Runnable {
            server.destroy()
        }))
    }

}