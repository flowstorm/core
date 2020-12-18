package org.promethist.core.servlets

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.promethist.common.servlets.InjectableWebSocketServlet
import org.promethist.core.socket.BotClientSocketAdapter
import javax.servlet.annotation.WebServlet

@WebServlet(name = "BotClient WebSocket Servlet", urlPatterns = ["/socket/", "/client/"])
class BotClientServlet : InjectableWebSocketServlet<BotClientSocketAdapter>() {

    override fun configure(factory: WebSocketServletFactory) {
        configure(factory, BotClientSocketAdapter::class.java)
    }

}