package com.promethist.port.servlets

import com.promethist.common.servlets.InjectableWebSocketServlet
import com.promethist.port.BotSocketAdapterV1
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.annotation.WebServlet

@WebServlet(name = "Bot WebSocket Servlet V1", urlPatterns = ["/channel/"])
class BotServletV1 : InjectableWebSocketServlet<BotSocketAdapterV1>() {

    override fun configure(factory: WebSocketServletFactory) {
        configure(factory, BotSocketAdapterV1::class.java)
    }
}
