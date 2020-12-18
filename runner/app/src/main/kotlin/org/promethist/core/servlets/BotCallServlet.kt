package org.promethist.core.servlets

import org.promethist.common.servlets.InjectableWebSocketServlet
import org.promethist.core.socket.BotCallSocketAdapter
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.annotation.WebServlet

@WebServlet(name = "BotCall WebSocket Servlet", urlPatterns = ["/call/"])
class BotCallServlet : InjectableWebSocketServlet<BotCallSocketAdapter>() {

    override fun configure(factory: WebSocketServletFactory) {
        configure(factory, BotCallSocketAdapter::class.java)
    }

}