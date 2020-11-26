package com.promethist.common.servlets

import com.promethist.common.JerseyApplication
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory

abstract class InjectableWebSocketServlet<T> : WebSocketServlet() {

    /**
     * Configures web socket adapter of specified class using custom dependency injection.
     */
    fun configure(factory: WebSocketServletFactory, adapterClass: Class<T>) {
        factory.register(adapterClass)
        val creator = factory.creator
        factory.creator = WebSocketCreator { servletUpgradeRequest, servletUpgradeResponse ->
            @Suppress("UNCHECKED_CAST")
            val webSocket = creator.createWebSocket(servletUpgradeRequest, servletUpgradeResponse) as T
            JerseyApplication.instance.injectionManager.inject(webSocket)
            webSocket
        }
    }
}