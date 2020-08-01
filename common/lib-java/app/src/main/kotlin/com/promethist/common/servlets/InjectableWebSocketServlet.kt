package com.promethist.common.servlets

import com.promethist.common.JerseyApplication
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.inject.Inject

abstract class InjectableWebSocketServlet<T> : WebSocketServlet() {

    interface DependencyInjector<T> {

        fun inject(webSocket: T)
    }

    /**
     * Configures web socket adapter of specified class using custom dependency injection.
     */
    fun configure(factory: WebSocketServletFactory, adapterClass: Class<T>, injector: DependencyInjector<T>) {
        factory.register(adapterClass)
        val creator = factory.creator
        factory.creator = WebSocketCreator { servletUpgradeRequest, servletUpgradeResponse ->
            @Suppress("UNCHECKED_CAST")
            val webSocket = creator.createWebSocket(servletUpgradeRequest, servletUpgradeResponse) as T
            injector.inject(webSocket)
            webSocket
        }
    }
    /**
     * Configures web socket adapter and injects resources from Jersey application
     * to its all instance fields annotated with @Inject
     */
    fun configure(factory: WebSocketServletFactory, adapterClass: Class<T>) {
        configure(factory, adapterClass, object : DependencyInjector<T> {
            override fun inject(webSocket: T) {
                inject(webSocket, adapterClass)
            }
        })
    }

    fun <T> inject(obj: T, type: Class<T>) {
        for (field in type.declaredFields) {
            if (field.isAnnotationPresent(Inject::class.java)) {
                field.set(obj, JerseyApplication.instance[field.type])
            }
        }
        if (type.superclass != null)
            inject(obj, type.superclass)
    }
}