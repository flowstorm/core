package com.promethist.port.servlets

import com.promethist.common.servlets.InjectableWebSocketServlet
import com.promethist.port.socket.BotClientSocketAdapter
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.annotation.WebServlet

@WebServlet(name = "BotClient WebSocket Servlet", urlPatterns = ["/socket/", "/client/"])
class BotClientServlet : InjectableWebSocketServlet<BotClientSocketAdapter>() {

    override fun configure(factory: WebSocketServletFactory) {
        configure(factory, BotClientSocketAdapter::class.java)
    }

}