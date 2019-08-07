package com.promethistai.port.servlets

import com.promethistai.common.servlets.InjectableWebSocketServlet
import com.promethistai.port.bot.BotWebSocket
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.annotation.WebServlet

@WebServlet(name = "Bot WebSocket Servlet", urlPatterns = ["/stream/bot", "/channel/"/*bronzerabbit compatibility*/])
class BotServlet : InjectableWebSocketServlet<BotWebSocket>() {

    override fun configure(factory: WebSocketServletFactory) {
        configure(factory, BotWebSocket::class.java)
    }

}