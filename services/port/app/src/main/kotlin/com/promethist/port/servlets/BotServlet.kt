package com.promethist.port.servlets

import com.promethist.common.servlets.InjectableWebSocketServlet
import com.promethist.port.BotSocketAdapter
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.annotation.WebServlet

@WebServlet(name = "Bot WebSocket Servlet", urlPatterns = ["/stream/bot", "/channel/"/*bronzerabbit compatibility*/])
class BotServlet : InjectableWebSocketServlet<BotSocketAdapter>() {

    override fun configure(factory: WebSocketServletFactory) {
        configure(factory, BotSocketAdapter::class.java)
    }

}